package com.droidfad.iframework.service;

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
 * ----------------------------------------------------------------------
 * all classes that use a service and have to be automatically 
 * initialized by DroidFad have to implement this interface
 */
public interface IServiceUser extends IServiceBase {
	/**
	 * inject a service to the service user. This method is called 
	 * once for every service in the framework for every IServiceUser
	 * on startup of the framework
	 * @param service
	 *
	 */
	public void registerService(IService service);
}
