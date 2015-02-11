/*
 * MIT License
 *
 * Copyright (c) 2014 Klemm Software Consulting, Mirko Klemm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.kscs.util.plugins.xjc.common;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kscs.util.plugins.xjc.ApiConstructs;
import com.kscs.util.plugins.xjc.Interface;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.fmt.JTextFile;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.EnumOutline;
import com.sun.tools.xjc.reader.Const;
import com.sun.xml.bind.v2.schemagen.episode.Bindings;
import com.sun.xml.bind.v2.schemagen.episode.SchemaBindings;
import com.sun.xml.txw2.TXW;
import com.sun.xml.txw2.output.StreamSerializer;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSDeclaration;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.XSXPath;
import com.sun.xml.xsom.visitor.XSFunction;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Mirko Klemm 2015-02-11
 */
public class EpisodeBuilder {
	private final ApiConstructs apiConstructs;
	private final String episodeFileName;
	private final List<OutlineAdaptor> outlines = new ArrayList<>();


	public EpisodeBuilder(final ApiConstructs apiConstructs, final String episodeFileName) {
		this.apiConstructs = apiConstructs;
		this.episodeFileName = episodeFileName;
	}

	public EpisodeBuilder addInterface(final XSDeclaration schemaComponent, final JDefinedClass iface) {
		this.outlines.add(new OutlineAdaptor(schemaComponent, OutlineAdaptor.OutlineType.INTERFACE, iface));
		return this;
	}

	public EpisodeBuilder addClass(final ClassOutline classOutline) {
		this.outlines.add(new OutlineAdaptor(classOutline.target.getSchemaComponent(), OutlineAdaptor.OutlineType.CLASS, classOutline.implClass));
		return this;
	}

	public EpisodeBuilder addEnum(final EnumOutline enumOutline) {
		this.outlines.add(new OutlineAdaptor(enumOutline.target.getSchemaComponent(), OutlineAdaptor.OutlineType.ENUM, enumOutline.clazz));
		return this;
	}

	/**
	 * Capture all the generated classes from global schema components
	 * and generate them in an episode file.
	 */
	public void build() throws SAXException {
		// reorganize qualifying components by their namespaces to
		// generate the list nicely
		final Map<XSSchema, PerSchemaOutlineAdaptors> perSchema = new LinkedHashMap<>();
		boolean hasComponentInNoNamespace = false;

		for (final OutlineAdaptor oa : outlines) {
			final XSComponent sc = oa.schemaComponent;

			if (sc != null && (sc instanceof XSDeclaration)) {
				final XSDeclaration decl = (XSDeclaration) sc;
				if (!decl.isLocal()) { // local components cannot be referenced from outside, so no need to list.
					PerSchemaOutlineAdaptors list = perSchema.get(decl.getOwnerSchema());
					if (list == null) {
						list = new PerSchemaOutlineAdaptors();
						perSchema.put(decl.getOwnerSchema(), list);
					}

					list.add(oa);

					if ("".equals(decl.getTargetNamespace()))
						hasComponentInNoNamespace = true;
				}
			}
		}

		//final File episodeFile = new File(opt.targetDir, this.episodePackageName.replaceAll("\\.", "/") + "/" + this.episodeFileName);
		try (final StringWriter stringWriter = new StringWriter()) {
			final Bindings bindings = TXW.create(Bindings.class, new StreamSerializer(stringWriter));
			if (hasComponentInNoNamespace) // otherwise jaxb binding NS should be the default namespace
				bindings._namespace(Const.JAXB_NSURI, "jaxb");
			else
				bindings._namespace(Const.JAXB_NSURI, "");
			bindings._namespace(Namespaces.KSCS_BINDINGS_NS, "kscs");
			bindings.version("2.1");
			bindings._comment("\n\n" + this.apiConstructs.opt.getPrologComment() + "\n  ");

			// generate listing per schema
			for (final Map.Entry<XSSchema, PerSchemaOutlineAdaptors> e : perSchema.entrySet()) {
				final PerSchemaOutlineAdaptors ps = e.getValue();
				final Bindings group = bindings.bindings();
				final String tns = e.getKey().getTargetNamespace();
				if (Namespaces.XML_NS.equals(tns)) {
					group._namespace(tns, "xml");
				} else if (!"".equals(tns)) {
					group._namespace(tns, "tns");
				}

				group.scd("x-schema::" + ("".equals(tns) ? "" : (Namespaces.XML_NS.equals(tns) ? "xml" : "tns")));
				group._attribute("if-exists", "true");

				final SchemaBindings schemaBindings = group.schemaBindings();
				schemaBindings.map(false);
				if (ps.packageNames.size() == 1) {
					final String packageName = ps.packageNames.iterator().next();
					if (packageName != null && packageName.length() > 0) {
						schemaBindings._package().name(packageName);
					}
				}

				for (final OutlineAdaptor oa : ps.outlineAdaptors) {
					final Bindings child = group.bindings();
					oa.buildBindings(child);
				}
				group.commit(true);
			}

			bindings.commit();

			final JTextFile jTextFile = new JTextFile(this.episodeFileName);
			jTextFile.setContents(stringWriter.toString());
			this.apiConstructs.codeModel.rootPackage().addResourceFile(jTextFile);
		} catch (final IOException e) {
			this.apiConstructs.errorHandler.error(new SAXParseException("Failed to write to " + this.episodeFileName, null, e));
		}
	}

	private static final class PerSchemaOutlineAdaptors {

		private final List<OutlineAdaptor> outlineAdaptors = new ArrayList<>();

		private final Set<String> packageNames = new HashSet<>();

		private void add(final OutlineAdaptor outlineAdaptor) {
			this.outlineAdaptors.add(outlineAdaptor);
			this.packageNames.add(outlineAdaptor.packageName);
		}

	}

	/**
	* @author Mirko Klemm 2015-02-11
	*/
	static final class OutlineAdaptor {

		/**
		 * Computes SCD.
		 * This is fairly limited as JAXB can only map a certain kind of components to classes.
		 */
		private static final XSFunction<String> SCD = new XSFunction<String>() {
			private String name(final XSDeclaration decl) {
				if ("".equals(decl.getTargetNamespace())) {
					return decl.getName();
				} else if(Namespaces.XML_NS.equals(decl.getTargetNamespace())) {
					return "xml:" + decl.getName();
				} else {
					return "tns:" + decl.getName();
				}
			}

			public String complexType(final XSComplexType type) {
				return "~" + name(type);
			}

			public String simpleType(final XSSimpleType simpleType) {
				return "~" + name(simpleType);
			}

			public String elementDecl(final XSElementDecl decl) {
				return name(decl);
			}

			public String modelGroupDecl(final XSModelGroupDecl decl) {
				return "group::"+name(decl);
			}

			public String attGroupDecl(final XSAttGroupDecl decl) {
				return "attributeGroup::"+name(decl);
			}

			// the rest is doing nothing
			public String annotation(final XSAnnotation ann) {
				throw new UnsupportedOperationException();
			}

			public String attributeDecl(final XSAttributeDecl decl) {
				throw new UnsupportedOperationException();
			}

			public String attributeUse(final XSAttributeUse use) {
				throw new UnsupportedOperationException();
			}

			public String schema(final XSSchema schema) {
				throw new UnsupportedOperationException();
			}

			public String facet(final XSFacet facet) {
				throw new UnsupportedOperationException();
			}

			public String notation(final XSNotation notation) {
				throw new UnsupportedOperationException();
			}

			public String identityConstraint(final XSIdentityConstraint decl) {
				throw new UnsupportedOperationException();
			}

			public String xpath(final XSXPath xpath) {
				throw new UnsupportedOperationException();
			}

			public String particle(final XSParticle particle) {
				throw new UnsupportedOperationException();
			}

			public String empty(final XSContentType empty) {
				throw new UnsupportedOperationException();
			}

			public String wildcard(final XSWildcard wc) {
				throw new UnsupportedOperationException();
			}

			public String modelGroup(final XSModelGroup group) {
				throw new UnsupportedOperationException();
			}
		};

		final XSComponent schemaComponent;
		final OutlineType outlineType;
		final String implName;
		final String packageName;

		public OutlineAdaptor(final XSComponent schemaComponent, final OutlineType outlineType,
		                      final String implName, final String packageName) {
			this.schemaComponent = schemaComponent;
			this.outlineType = outlineType;
			this.implName = implName;
			this.packageName = packageName;
		}

		public OutlineAdaptor(final XSComponent schemaComponent, final OutlineType outlineType,
		                      final JDefinedClass javaType) {
			this.schemaComponent = schemaComponent;
			this.outlineType = outlineType;
			this.implName = javaType.fullName();
			this.packageName = javaType.getPackage().name();
		}

		void buildBindings(final Bindings bindings) {
			bindings.scd(this.schemaComponent.apply(OutlineAdaptor.SCD));
			this.outlineType.bindingsBuilder.build(this, bindings);
		}

		public enum OutlineType {

			CLASS(new BindingsBuilder() {
				public void build(final OutlineAdaptor adaptor, final Bindings bindings) {
					bindings.klass().ref(adaptor.implName);
				}
			}),
			ENUM(new BindingsBuilder() {
				public void build(final OutlineAdaptor adaptor, final Bindings bindings) {
					bindings.typesafeEnumClass().ref(adaptor.implName);
				}
			}),
			INTERFACE(new BindingsBuilder() {
							public void build(final OutlineAdaptor adaptor, final Bindings bindings) {
								bindings._element(Namespaces.KSCS_BINDINGS_NS, "interface", Interface.class).ref(adaptor.implName);
							}
						});

			public final BindingsBuilder bindingsBuilder;

			private OutlineType(final BindingsBuilder bindingsBuilder) {
				this.bindingsBuilder = bindingsBuilder;
			}

			public interface BindingsBuilder {
				void build(OutlineAdaptor adaptor, Bindings bindings);
			}

		}
	}
}
