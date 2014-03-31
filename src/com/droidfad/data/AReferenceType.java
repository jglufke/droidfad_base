/**
 * 
 */
package com.droidfad.data;


/**
Copyright 2014 Jens Glufke

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
public abstract class AReferenceType extends ADao {

	public enum Cardinality { _1to1, _1toN, _NtoM, _Nto1 }

	/**
	 * typeId is used to reduce persistent storage size of
	 * _Reference istances. They only have to store the 
	 * typeId of the respective reference and not the complete
	 * class name. A short should be enough.
	 */
	public enum Attributes implements AttributesEnum { 
		Cardinality, SourceType, SourceGuiName, TargetType, TargetGuiName  
	}

	private Class<? extends ADao> sourceType;
	private Class<? extends ADao> targetType;
	private Cardinality cardinality;
	private String targetGuiName;
	private String sourceGuiName;

	/**
	 * ********************************************<br>
	 *
	 * @param pName
	 *
	 * ********************************************<br>
	 */
	protected AReferenceType(String pName, 
			Class<? extends ADao> pSourceType, String pSourceGuiName,
			Class<? extends ADao> pTargetType, String pTargetGuiName,
			Cardinality pCardinality) {
		super(pName);

		sourceType    = pSourceType;
		sourceGuiName = pSourceGuiName;
		targetType    = pTargetType;
		targetGuiName = pTargetGuiName;
		cardinality   = pCardinality;
	}
	/**
	 * get the ADao subclass this reference points from
	 *
	 * @return
	 *
	 */
	public Class<? extends ADao> getSourceType() {
		return sourceType;
	}
	/**
	 * get the ADao subclass this reference points to
	 *
	 * @return
	 *
	 */
	public Class<? extends ADao> getTargetType() {
		return targetType;
	}

	/**
	 * return the name that should be displayed in relation's target direction
	 * in the ui
	 *
	 * @return
	 *
	 */
	public String getTargetGuiName() {
		return targetGuiName;
	}
	/**
	 * 
	 * return the name that should be displayed in relation's source direction
	 * in the ui
	 * @return
	 *
	 */
	public String getSourceGuiName() {
		return sourceGuiName;
	}
	public Cardinality getCardinality() {
		return cardinality;
	}
}
