package com.droidfad.iframework.service;


/**
 * 
 *
 * Copyright 2011 Jens Glufke jglufke@googlemail.com
 *
 *   Licensed under the DROIDFAD license (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.droidfad.com/html/license/license.htm
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.<br>
 * -----------------------------------------------------------------------<br>--------
 * all services that have to be handled by DroidFad have to implement this interface.<br>
 * Following rules apply for the implementation of services<br>
 * I.   A Service exists as a single instance in the framework and is instantiated<br>
 *      by the framework<br>
 * II.  A Service has to implement the default constructor<br>
 * 
 * @author Jens
 *
 * @param <T>
 */
public interface IService extends IServiceBase {

}
