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

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
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

/**
 * @author Mirko Klemm 2015-02-11
 */
public abstract class AbstractXSFunction<T> implements XSFunction<T> {
	@Override
	public T annotation(final XSAnnotation ann) {
		return null;
	}

	@Override
	public T attGroupDecl(final XSAttGroupDecl decl) {
		return null;
	}

	@Override
	public T attributeDecl(final XSAttributeDecl decl) {
		return null;
	}

	@Override
	public T attributeUse(final XSAttributeUse use) {
		return null;
	}

	@Override
	public T complexType(final XSComplexType type) {
		return null;
	}

	@Override
	public T schema(final XSSchema schema) {
		return null;
	}

	@Override
	public T facet(final XSFacet facet) {
		return null;
	}

	@Override
	public T notation(final XSNotation notation) {
		return null;
	}

	@Override
	public T identityConstraint(final XSIdentityConstraint decl) {
		return null;
	}

	@Override
	public T xpath(final XSXPath xpath) {
		return null;
	}

	@Override
	public T simpleType(final XSSimpleType simpleType) {
		return null;
	}

	@Override
	public T particle(final XSParticle particle) {
		return null;
	}

	@Override
	public T empty(final XSContentType empty) {
		return null;
	}

	@Override
	public T wildcard(final XSWildcard wc) {
		return null;
	}

	@Override
	public T modelGroupDecl(final XSModelGroupDecl decl) {
		return null;
	}

	@Override
	public T modelGroup(final XSModelGroup group) {
		return null;
	}

	@Override
	public T elementDecl(final XSElementDecl decl) {
		return null;
	}
}
