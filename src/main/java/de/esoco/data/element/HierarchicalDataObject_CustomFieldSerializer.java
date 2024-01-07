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

/********************************************************************
 * A GWT custom field serializer for the {@link HierarchicalDataObject} class.
 *
 * @author eso
 */
public class HierarchicalDataObject_CustomFieldSerializer
	extends CustomFieldSerializer<HierarchicalDataObject> {
	//~ Static methods
	// ---------------------------------------------------------

	/***************************************
	 * Not used, implemented in {@link #instantiate(SerializationStreamReader)}
	 * instead.
	 *
	 * @param  rReader The stream reader to read the object data from
	 * @param  rObject The object to de-serialize
	 *
	 * @throws SerializationException If the stream access fails
	 */
	public static void deserialize(SerializationStreamReader rReader,
		HierarchicalDataObject rObject) throws SerializationException {
	}

	/***************************************
	 * Restores the complete object hierarchy from a serialization stream.
	 *
	 * @param  rReader The stream reader to read the object data from
	 *
	 * @return The restored object
	 *
	 * @throws SerializationException If the stream access fails
	 */
	public static HierarchicalDataObject instantiate(
		SerializationStreamReader rReader) throws SerializationException {
		String sId = rReader.readString();
		int nIndex = rReader.readInt();
		int nCount = rReader.readInt();
		boolean bReadonly = rReader.readBoolean();
		List<String> aValues = new ArrayList<String>(nCount);
		Set<String> aFlags = null;
		HierarchicalDataObject rResult;

		for (int i = 0; i < nCount; i++) {
			aValues.add(rReader.readString());
		}

		nCount = rReader.readInt();

		if (nCount > 0) {
			aFlags = new HashSet<String>(nCount);

			for (int i = 0; i < nCount; i++) {
				aFlags.add(rReader.readString());
			}
		}

		nCount = rReader.readInt();

		if (nCount < 0) {
			@SuppressWarnings("unchecked")
			DataModel<DataModel<String>> aChildren =
				(DataModel<DataModel<String>>) rReader.readObject();

			rResult =
				new HierarchicalDataObject(sId, nIndex, aValues, bReadonly,
					aFlags, aChildren);
		} else {
			List<DataModel<String>> aChildren = null;

			if (nCount > 0) {
				aChildren = new ArrayList<DataModel<String>>(nCount);

				for (int i = 0; i < nCount; i++) {
					aChildren.add(
						(HierarchicalDataObject) rReader.readObject());
				}
			}

			rResult =
				new HierarchicalDataObject(sId, nIndex, aValues, bReadonly,
					aFlags, aChildren);
		}

		return rResult;
	}

	/***************************************
	 * Writes the complete object hierarchy to a serialization stream.
	 *
	 * @param  rWriter The stream writer to write the object data to
	 * @param  rObject The object to serialize
	 *
	 * @throws SerializationException If the stream access fails
	 */
	public static void serialize(SerializationStreamWriter rWriter,
		HierarchicalDataObject rObject) throws SerializationException {
		rWriter.writeString(rObject.id);
		rWriter.writeInt(rObject.index);
		rWriter.writeInt(rObject.values.size());
		rWriter.writeBoolean(rObject.editable);

		for (String sValue : rObject) {
			rWriter.writeString(sValue);
		}

		Collection<String> rFlags = rObject.getFlags();

		rWriter.writeInt(rFlags.size());

		for (String sFlag : rFlags) {
			rWriter.writeString(sFlag);
		}

		if (rObject.children != null) {
			if (rObject.children instanceof ListDataModel) {
				rWriter.writeInt(rObject.children.getElementCount());

				for (DataModel<String> rChild : rObject.children) {
					rWriter.writeObject(rChild);
				}
			} else {
				rWriter.writeInt(-1);
				rWriter.writeObject(rObject.children);
			}
		} else {
			rWriter.writeInt(0);
		}
	}

	//~ Methods
	// ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void deserializeInstance(SerializationStreamReader rStreamReader,
		HierarchicalDataObject rInstance) throws SerializationException {
		deserialize(rStreamReader, rInstance);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasCustomInstantiateInstance() {
		return true;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public HierarchicalDataObject instantiateInstance(
		SerializationStreamReader rStreamReader) throws SerializationException {
		return instantiate(rStreamReader);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void serializeInstance(SerializationStreamWriter rStreamWriter,
		HierarchicalDataObject rInstance) throws SerializationException {
		serialize(rStreamWriter, rInstance);
	}
}
