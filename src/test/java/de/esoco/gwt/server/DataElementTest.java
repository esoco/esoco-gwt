//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-gwt' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.gwt.server;

import de.esoco.data.element.BooleanDataElement;
import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElement.Flag;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.StringDataElement;
import de.esoco.data.validate.RegExValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test case for data elements
 *
 * @author eso
 */
public class DataElementTest {

	// ------------------------------------------------------------------

	/**
	 * Test enum
	 */
	enum TestEnum {T1, T2, T3, T4}

	// ----------------------------------------------------------------

	/**
	 * Test attribute query methods.
	 */
	@Test
	public void testAttributes() {
		StringDataElement e = new StringDataElement("TEST", "VALUE");

		assertEquals("TEST", e.getName());
		assertEquals("/TEST", e.getPath());
		assertEquals("VALUE", e.getValue());
		assertFalse(e.isImmutable());
		assertFalse(e.isSelected());
		assertFalse(e.isModified());
		assertNull(e.getParent());

		e.setValue("NEW");
		assertEquals("NEW", e.getValue());
		assertTrue(e.isModified());
		assertFalse(e.isSelected());
		assertFalse(e.isImmutable());

		e.setSelected(true);
		assertTrue(e.isSelected());
		e.setSelected(false);
		assertFalse(e.isSelected());
	}

	/**
	 * Test of
	 * {@link DataElementFactory#createEnumDataElement(String, Class, Object,
	 * java.util.Collection, java.util.Set)}.
	 */
	@Test
	public void testEnum() {
		DataElementFactory aFactory = new DataElementFactory(null);

		DataElement<?> e1 =
			aFactory.createEnumDataElement("TEST", TestEnum.class, TestEnum.T1,
				null, null);
		DataElement<?> e1a =
			aFactory.createEnumDataElement("TEST", TestEnum.class, TestEnum.T1,
				null, null);
		DataElement<?> e2 =
			aFactory.createEnumDataElement("TEST2", TestEnum.class,
				TestEnum.T2,
				null, null);

		assertEquals(e1, e1a);
		assertNotEquals(e1, e2);
		assertEquals(TestEnum.T1.name(), e1.getValue());
		assertEquals(TestEnum.T2.name(), e2.getValue());
	}

	/**
	 * Test method
	 */
	@Test
	public void testEquals() {
		StringDataElement e1 = new StringDataElement("TEST", "VALUE");
		StringDataElement e2 = new StringDataElement("TEST", "VALUE");
		StringDataElement e3 = new StringDataElement("TEST!", "VALUE");

		assertEquals(e1, e2);
		assertNotEquals(e1, e3);
		e2.setValue("VALUE");
		assertEquals(e1, e2);
		e2.setSelected(true);
		assertNotEquals(e1, e2);
		e2.setValue("OTHER");
		assertNotEquals(e1, e2);
		assertNotEquals(e1, new Object());
		assertNotEquals(null, e1);
	}

	/**
	 * Test method
	 */
	@Test
	public void testHierarchy() {
		DataElementList root = new DataElementList("ROOT", null, null, null);
		DataElementList sub = new DataElementList("SUB", null, null, null);

		assertEquals(0, root.getElementCount());

		addElements(sub, "S", "SVAL", 3);
		assertEquals(3, sub.getElementCount());
		assertEquals("S1", sub.getElement(0).getName());
		assertEquals("S3", sub.getElement(2).getName());

		root.addElement(sub);
		assertEquals(1, root.getElementCount());
		assertEquals("SUB", root.getElement(0).getName());

		addElements(root, "R", "RVAL", 3);
		assertEquals(4, root.getElementCount());
		assertEquals("R1", root.getElement(1).getName());
		assertEquals("R3", root.getElement(3).getName());

		DataElement<?> r2 = root.getElement(2);
		DataElement<?> s2 = sub.getElement(1);

		assertTrue(root.containsElement(r2));
		assertTrue(sub.containsElement(s2));

		assertNull(root.getRoot());
		assertEquals(root, sub.getRoot());
		assertEquals(root, r2.getRoot());
		assertEquals(root, s2.getRoot());

		assertEquals("R2", r2.getName());
		assertEquals("S2", s2.getName());
		assertEquals("/ROOT/R2", r2.getPath());
		assertEquals("/ROOT/SUB", sub.getPath());
		assertEquals("/ROOT/SUB/S2", s2.getPath());
		assertEquals(root, root.getElementAt("/ROOT"));
		assertEquals("RVAL2", root.getElementAt("R2").getValue());
		assertEquals("RVAL2", root.getElementAt("/ROOT/R2").getValue());
		assertEquals("SVAL2", root.getElementAt("SUB/S2").getValue());
		assertEquals("SVAL2", root.getElementAt("/ROOT/SUB/S2").getValue());
		assertEquals("SVAL2", sub.getElementAt("S2").getValue());
		assertEquals("SVAL2", sub.getElementAt("/SUB/S2").getValue());

		root.removeElement(r2);
		assertEquals(3, root.getElementCount());
		sub.removeElement(s2);
		assertEquals(2, sub.getElementCount());
	}

	/**
	 * Test method
	 */
	@Test
	public void testReadOnly() {
		EnumSet<Flag> aImmutableFlag = EnumSet.of(Flag.IMMUTABLE);

		StringDataElement e =
			new StringDataElement("ELEMENT", "VALUE", null, aImmutableFlag);
		DataElementList l =
			new DataElementList("PARENT", null, Collections.singletonList(e),
				aImmutableFlag);

		try {
			e.setValue("X");
			fail();
		} catch (UnsupportedOperationException ex) { // this should happen
		}

		try {
			l.removeElement(e);
			fail();
		} catch (UnsupportedOperationException ex) { // this should happen
		}

		try {
			l.addElement(new BooleanDataElement("X", Boolean.TRUE, null));
			fail();
		} catch (UnsupportedOperationException ex) { // this should happen
		}
	}

	/**
	 * Test method
	 */
	@Test
	public void testValidValue() {
		StringDataElement e =
			new StringDataElement("TEST", "1", new RegExValidator("\\d+"),
				null);

		assertTrue(e.isValidValue(e.getValidator(), "2"));
		assertFalse(e.isValidValue(e.getValidator(), ""));
		assertFalse(e.isValidValue(e.getValidator(), "a"));

		e.setValue("123");
		assertEquals("123", e.getValue());

		try {
			e.setValue("abc");
			fail();
		} catch (IllegalArgumentException ex) { // this should happen
		}
	}

	/**
	 * Helper method to fill a data element list.
	 *
	 * @param rList        rParent The parent list to add the elements to
	 * @param sNamePrefix  The prefix for the element names
	 * @param sValuePrefix The prefix for the values
	 * @param nCount       The number of elements to add
	 */
	private void addElements(DataElementList rList, String sNamePrefix,
		String sValuePrefix, int nCount) {
		for (int i = 1; i <= nCount; i++) {
			rList.addElement(
				new StringDataElement(sNamePrefix + i, sValuePrefix + i));
		}
	}
}
