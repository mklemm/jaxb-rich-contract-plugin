/*
 * GNU General Public License
 *
 * Copyright (c) 2018 Klemm Software Consulting, Mirko Klemm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kscs.util.plugins.xjc.outline;

import java.util.List;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;

public class ReferencedStubClassOutline implements TypeOutline {
	private final JCodeModel codeModel;
	private final JClass clazz;
	private final TypeOutline superclass;

	public ReferencedStubClassOutline(final JCodeModel codeModel, final JClass clazz) {
		this.codeModel = codeModel;
		this.clazz = clazz;
		this.superclass = null;
	}

	@Override
	public List<? extends PropertyOutline> getDeclaredFields() {
		return List.of();
	}

	@Override
	public TypeOutline getSuperClass() {
		return this.superclass;
	}

	@Override
	public JClass getImplClass() {
		return this.clazz;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isInterface() {
		return false;
	}
}
