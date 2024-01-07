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
package de.esoco.lib.property;

import de.esoco.data.element.DataElement;
import de.esoco.data.element.DataElementList;
import de.esoco.data.element.DataSetDataElement;
import de.esoco.data.element.DateDataElement;
import de.esoco.data.element.DateListDataElement;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * A GWT custom field serializer for the {@link PropertyName} class that
 * restores property names as singletons.
 *
 * @author eso
 */
public class PropertyName_CustomFieldSerializer {

	static {
		// initializes property name instances defined in this classes
		StandardProperties.init();
		StorageProperties.init();
		UserInterfaceProperties.init();
		DataElement.init();
		DataElementList.init();
		DateDataElement.init();
		DateListDataElement.init();
		DataSetDataElement.init();
	}

	/**
	 * Not used, implemented in {@link #instantiate(SerializationStreamReader)}
	 * instead.
	 *
	 * @param rReader The stream reader to read the object data from
	 * @param rObject The object to de-serialize
	 * @throws SerializationException If the stream access fails
	 */
	public static void deserialize(SerializationStreamReader rReader,
		PropertyName<?> rObject) throws SerializationException {
	}

	/**
	 * Restores the property name from a serialization stream.
	 *
	 * @param rReader The stream reader to read the object data from
	 * @return The restored object
	 * @throws SerializationException If the stream access fails
	 */
	public static PropertyName<?> instantiate(SerializationStreamReader rReader)
		throws SerializationException {
		String sName = rReader.readString();
		PropertyName<?> rPropertyName = PropertyName.valueOf(sName);

		if (rPropertyName == null) {
			throw new IllegalStateException(
				"No PropertyName instance for " + sName);
		}

		return rPropertyName;
	}

	/**
	 * Writes the name string to the stream.
	 *
	 * @param rWriter       The stream writer to write the object data to
	 * @param rPropertyName The object to serialize
	 * @throws SerializationException If the stream access fails
	 */
	public static void serialize(SerializationStreamWriter rWriter,
		PropertyName<?> rPropertyName) throws SerializationException {
		rWriter.writeString(rPropertyName.getName());
	}
}
