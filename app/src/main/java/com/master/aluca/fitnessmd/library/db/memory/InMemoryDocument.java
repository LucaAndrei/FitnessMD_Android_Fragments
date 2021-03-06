/*********************************************************
 *
 * Copyright (c) 2017 Andrei Luca
 * All rights reserved. You may not copy, distribute, publicly display,
 * create derivative works from or otherwise use or modify this
 * software without first obtaining a license from Andrei Luca
 *
 *********************************************************/

package com.master.aluca.fitnessmd.library.db.memory;

/*
 * Copyright (c) delight.im <info@delight.im>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.master.aluca.fitnessmd.library.Fields;

import java.util.HashMap;

/** Document that is stored in memory */
public final class InMemoryDocument {

	/** The ID of the document */
	private final String mId;
	/** The fields of the document */
	private final Fields mFields;

	/**
	 * Creates a new document that is stored in memory
	 *
	 * @param id the ID of the document to create
	 * @param fields the initial fields for the document to create
	 */
	protected InMemoryDocument(final String id, final Fields fields) {
		mId = id;
		mFields = fields;
	}

	public String getId() {
		return mId;
	}

	public Object getField(final String name) {
		return mFields.get(name);
	}

	public String[] getFieldNames() {
		return mFields.keySet().toArray(new String[mFields.size()]);
	}

	/**
	 * Returns the raw map of fields backing this document
	 *
	 * @return the raw map of fields
	 */
	protected Fields getFields() {
		return mFields;
	}
}
