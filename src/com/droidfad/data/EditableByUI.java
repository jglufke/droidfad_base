/**
 * 
 */
package com.droidfad.data;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 
 *
 * Copyright 2011 Jens Glufke jglufke@googlemail.com

   Licensed under the DROIDFAD license (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.droidfad.com/html/license/license.htm

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.<br>
 * -----------------------------------------------------------------------<br>
 * adding this annotation to a getter or setter of an attribute will make
 * the attribute editable by the {@link Editor} ui element
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface EditableByUI {
	/**
	 * 
	 * the sorting tag defines the order in which the
	 * several fields in an editor will appear.
	 * the tag is sorted as string
	 *
	 */
	public String sortingTag() default "";
	/**
	 * 
	 * GuiName defines what is displayed as text for
	 * the editable field in the editor. It can be
	 * a resource id to a string. If no string resource
	 * has been found the string is taken itself
	 *
	 */
	public String guiName()    default "";
} 
