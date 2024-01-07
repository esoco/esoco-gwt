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
package de.esoco.data.element;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import de.esoco.lib.model.DataModel;
import de.esoco.lib.model.ListDataModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A GWT custom field serializer for the {@link HierarchicalDataObject} class.
 *
 * @author eso
 */
public class HierarchicalDataObject_CustomFieldSerializer
	extends CustomFieldSerializer<HierarchicalDataObject> {

	// ---------------------------------------------------------

	/**
	 * Not used, implemented in {@link #instantiate(SerializationStreamReader)}
	 * instead.
	 *
	 * @param reader The stream reader to read the object data from
	 * @param object The object to de-serialize
	 * @throws SerializationException If the stream access fails
	 */
	public static void deserialize(SerializationStreamReader reader,
		HierarchicalDataObject object) throws SerializationException {
	}

	/**
	 * Restores the complete object hierarchy from a serialization stream.
	 *
	 * @param reader The stream reader to read the object data from
	 * @return The restored object
	 * @throws SerializationException If the stream access fails
	 */
	public static HierarchicalDataObject instantiate(
		SerializationStreamReader reader) throws SerializationException {
		String id = reader.readString();
		int index = reader.readInt();
		int count = reader.readInt();
		boolean readonly = reader.readBoolean();
		List<String> values = new ArrayList<String>(count);
		Set<String> flags = null;
		HierarchicalDataObject result;

		for (int i = 0; i < count; i++) {
			values.add(reader.readString());
		}

		count = reader.readInt();

		if (count > 0) {
			flags = new HashSet<String>(count);

			for (int i = 0; i < count; i++) {
				flags.add(reader.readString());
			}
		}

		count = reader.readInt();

		if (count < 0) {
			@SuppressWarnings("unchecked")
			DataModel<DataModel<String>> children =
				(DataModel<DataModel<String>>) reader.readObject();

			result =
				new HierarchicalDataObject(id, index, values, readonly, flags,
					children);
		} else {
			List<DataModel<String>> children = null;

			if (count > 0) {
				children = new ArrayList<DataModel<String>>(count);

				for (int i = 0; i < count; i++) {
					children.add((HierarchicalDataObject) reader.readObject());
				}
			}

			result =
				new HierarchicalDataObject(id, index, values, readonly, flags,
					children);
		}

		return result;
	}

	/**
	 * Writes the complete object hierarchy to a serialization stream.
	 *
	 * @param writer The stream writer to write the object data to
	 * @param object The object to serialize
	 * @throws SerializationException If the stream access fails
	 */
	public static void serialize(SerializationStreamWriter writer,
		HierarchicalDataObject object) throws SerializationException {
		writer.writeString(object.id);
		writer.writeInt(object.index);
		writer.writeInt(object.values.size());
		writer.writeBoolean(object.editable);

		for (String value : object) {
			writer.writeString(value);
		}

		Collection<String> flags = object.getFlags();

		writer.writeInt(flags.size());

		for (String flag : flags) {
			writer.writeString(flag);
		}

		if (object.children != null) {
			if (object.children instanceof ListDataModel) {
				writer.writeInt(object.children.getElementCount());

				for (DataModel<String> child : object.children) {
					writer.writeObject(child);
				}
			} else {
				writer.writeInt(-1);
				writer.writeObject(object.children);
			}
		} else {
			writer.writeInt(0);
		}
	}

	// ----------------------------------------------------------------

	@Override
	public void deserializeInstance(SerializationStreamReader streamReader,
		HierarchicalDataObject instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public boolean hasCustomInstantiateInstance() {
		return true;
	}

	@Override
	public HierarchicalDataObject instantiateInstance(
		SerializationStreamReader streamReader) throws SerializationException {
		return instantiate(streamReader);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter,
		HierarchicalDataObject instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
