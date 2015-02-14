/*******************************************************************************
 * Copyright (c) 2010 Luaj.org. All rights reserved.
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
 ******************************************************************************/

package org.luaj.vm2.luajc;

import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;

import java.io.IOException;
import java.io.InputStream;

/**
 * Alternative version of LuaJC which fixes class names properly.
 * For instance chunk-name breaks in LuaJC
 */
public class LuaJCRewrite implements LoadState.LuaCompiler {
	protected static final String NON_IDENTIFIER = "[^a-zA-Z0-9_]";

	protected static LuaJCRewrite instance;

	public static LuaJCRewrite getInstance() {
		if (instance == null) instance = new LuaJCRewrite();
		return instance;
	}

	/**
	 * Install the compiler as the main compiler to use.
	 */
	public static void install() {
		LoadState.compiler = getInstance();
	}

	public LuaFunction load(InputStream stream, String name, LuaValue env) throws IOException {
		Prototype p = LuaC.compile(stream, name);
		String className = toStandardJavaClassName(name);

		try {
			JavaLoaderRewrite loader = new JavaLoaderRewrite(env);
			return loader.load(p, className, name);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static String toStandardJavaClassName(String chunkName) {
		String stub = chunkName.endsWith(".lua") ? chunkName.substring(0, chunkName.length() - 4) : chunkName;
		String className = stub.replace('/', '.').replaceAll(NON_IDENTIFIER, "_");

		int c = className.charAt(0);
		if (c != '_' && !Character.isJavaIdentifierStart(c)) className = "_" + className;

		return className + "_LuaCompiled";
	}
}
