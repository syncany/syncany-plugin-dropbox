/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

public class StringUtilTest {
	@Test 
	public void testFromHexToHex() {
		assertEquals("abcdeffaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", StringUtil.toHex(StringUtil.fromHex("abcdeffaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));
		assertEquals("", StringUtil.toHex(StringUtil.fromHex("")));		
	}
	
	@Test(expected=Exception.class)
	public void testFromHexInvalid1() {
		StringUtil.toHex(StringUtil.fromHex("a"));
	}
	
	@Test(expected=Exception.class)
	public void testFromHexInvalid2() {
		StringUtil.toHex(StringUtil.fromHex("INVALID!"));
	}
	
	@Test(expected=Exception.class)
	public void testFromHexInvalid3() {
		StringUtil.toHex(StringUtil.fromHex("INVALID"));
	}
	
	@Test
	public void testStringJoin() {
		assertEquals("a;b;c", StringUtil.join(new String[] { "a",  "b", "c" }, ";"));
		assertEquals("a b c", StringUtil.join(Arrays.asList(new String[] { "a",  "b", "c" }), " "));
		assertEquals("1.9 + 2.8 + 3.7", StringUtil.join(new Double[] { 1.911, 2.833, 3.744 }, " + ", new StringJoinListener<Double>() {
			@Override
			public String getString(Double number) {
				return String.format("%.1f", number);
			}			
		}));
	}
	
	@Test
	public void testToBytesUTF8() {
		assertArrayEquals(new byte[] { 0x00 }, StringUtil.toBytesUTF8("\0"));
		assertArrayEquals(new byte[] { (byte) 0xc3, (byte) 0xa4, (byte) 0xc3, (byte) 0xb6, (byte) 0xc3, (byte) 0xbc }, StringUtil.toBytesUTF8("äöü"));
		assertArrayEquals(new byte[] { (byte) 0xe7, (byte) 0xac, (byte) 0xaa, (byte) 0xe9, (byte) 0xaa, (byte) 0x8f }, StringUtil.toBytesUTF8("笪骏")); 
	}
	
	@Test
	public void testToCamelCase() {
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello world"));
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello_world"));
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello-world"));
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello-World"));
	}
	
	@Test
	public void testToSnakeCase() {
		assertEquals("hello_world", StringUtil.toSnakeCase("hello world"));
		assertEquals("hello_world", StringUtil.toSnakeCase("HelloWorld"));
		assertEquals("hello_world", StringUtil.toSnakeCase("helloWorld"));
		assertEquals("hello_world", StringUtil.toSnakeCase("hello_world"));
		assertEquals("s3", StringUtil.toSnakeCase("s3"));		
	}
	
	@Test
	public void testSubstrCount() {
		assertEquals(1, StringUtil.substrCount("some/path", "/"));
		assertEquals(2, StringUtil.substrCount("some/path/", "/"));
		assertEquals(1, StringUtil.substrCount("annanna", "anna"));
	}
	
	@Test
	public void testSplitCommandLineArgs() {
		String command = "arg1 \"arg2-1 arg2-2\" \"arg3-1-with-quote\\\" arg3-2\" \"arg4\\\"\" 'arg5' 'arg6\\'' '\"arg7\"' 'arg8-1 \"arg8-2\" arg8-3'";
		List<String> args = StringUtil.splitCommandLineArgs(command);
		
		assertEquals(8, args.size());
		assertEquals("arg1", args.get(0));
		assertEquals("arg2-1 arg2-2", args.get(1));
		assertEquals("arg3-1-with-quote\" arg3-2", args.get(2));
		assertEquals("arg4\"", args.get(3));
		assertEquals("arg5", args.get(4));		
		assertEquals("arg6'", args.get(5));
		assertEquals("\"arg7\"", args.get(6));		
		assertEquals("arg8-1 \"arg8-2\" arg8-3", args.get(7));		
	}
}
