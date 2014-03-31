package com.droidfad.service;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;

import com.droidfad.classloading.ClazzFinder;
import com.droidfad.iframework.service.IService;
import com.droidfad.iframework.service.IServiceBase;
import com.droidfad.iframework.service.IServiceUser;
import com.droidfad.util.LogWrapper;

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
 * -----------------------------------------------------------------------<br>
 *
 * responsible for discovering implementations of IService 
 * and to register these services at the respective service users. Initialization 
 * of services is called by DroidFad.init method. So, an application should not use
 * this class directly
 */
public class ServiceAdministrator {

	private final static String LOGTAG = ServiceAdministrator.class.getSimpleName();
	
	private HashSet<Class<? extends IServiceBase>> serviceBaseSet;

	private HashSet<IServiceBase> serviceBaseInstanceSet;

	private static ServiceAdministrator instance;

	private ServiceAdministrator() {}
	/**
	 *
	 */
	public static void dispose() {
		getInstance().serviceBaseSet         = null;
		getInstance().serviceBaseInstanceSet = null;
	}
	/**
	 * 
	 *
	 * @return
	 *
	 */
	public static synchronized ServiceAdministrator getInstance() {

		if(instance == null) {
			instance = new ServiceAdministrator();
		}
		return instance;
	}

	/**
	 * @param pActivity 
	 * 
	 */
	public void registerServices() {

		Vector<IServiceBase> lServiceBaseList = getServiceBaseList();
		Vector<IService>     lServiceList     = getServiceList(lServiceBaseList);
		Vector<IServiceUser> lServiceUserList = getServiceUserList(lServiceBaseList);

		registerServices(lServiceList, lServiceUserList);
	}

	/**
	 * 
	 * @param pServiceList
	 * @param pServiceUserList
	 */
	private void registerServices(Vector<IService> pServiceList,
			Vector<IServiceUser> pServiceUserList) {

		for(IService lService : pServiceList) {
			for(IServiceUser lServiceUser : pServiceUserList) {
				synchronized (this) {
					lServiceUser.registerService(lService);
				}
			}
		}
	}

	/**
	 * 
	 * @param pActivity 
	 * @return
	 */
	private Vector<IServiceBase> getServiceBaseList() {
		if(serviceBaseSet == null) {
			serviceBaseSet = ClazzFinder.findSubclasses(IServiceBase.class);
		}
		if(serviceBaseInstanceSet == null) {
			serviceBaseInstanceSet = new HashSet<IServiceBase>();
			for(Class<? extends IServiceBase> lClazz : serviceBaseSet) {

				String   lName  = lClazz.getName();
				/**
				 * check that only the default constructor is implemented
				 */
				if(!lClazz.isInterface() && (lClazz.getModifiers() & Modifier.ABSTRACT) == 0) {
					try {
						IServiceBase   lServiceBase = (IServiceBase) lClazz.newInstance();
						serviceBaseInstanceSet.add(lServiceBase);
					} catch (SecurityException e) {
						LogWrapper.e(LOGTAG, lName+ ":" + e);
					} catch (IllegalArgumentException e) {
						LogWrapper.e(LOGTAG, lName+ ":" + e);
					} catch (InstantiationException e) {
						LogWrapper.e(LOGTAG, "default constructor for service class:" + lName+ 
								" not found. Class must implement default constructor:" + e.getMessage());
					} catch (IllegalAccessException e) {
						LogWrapper.e(LOGTAG, "default constructor for service class:" + lName+ 
								" can not be invoked. Class must be declared public!:" + e.getMessage());
					}
				}
			}
		}
		return new Vector<IServiceBase>(Arrays.asList(serviceBaseInstanceSet.toArray(new IServiceBase[0])));
	}

	/**
	 * 
	 * @param pServiceBaseList
	 * @return
	 */
	private Vector<IServiceUser> getServiceUserList(Vector<IServiceBase> pServiceBaseList) {
		Vector<IServiceUser> lServiceUserList = new Vector<IServiceUser>();
		for(IServiceBase lServiceBase : pServiceBaseList) {
			if(lServiceBase instanceof IServiceUser) {
				lServiceUserList.add((IServiceUser) lServiceBase);
			}
		}
		return lServiceUserList;
	}

	/**
	 * 
	 * @param pServiceBaseList
	 * @return
	 */
	private Vector<IService> getServiceList(Vector<IServiceBase> pServiceBaseList) {
		Vector<IService> lServiceList = new Vector<IService>();
		for(IServiceBase lServiceBase : pServiceBaseList) {
			if(lServiceBase instanceof IService) {
				lServiceList.add((IService) lServiceBase);
			}
		}
		return lServiceList;
	}

	/**
	 * 
	 * @param pClass
	 * @param pActivity 
	 * @return
	 */
	public static IService getService(Class<? extends IService> pClass) {
		IService lService = null;
		if(pClass != null) {
			Vector<IServiceBase> lServiceBaseList = getInstance().getServiceBaseList();
			for(IServiceBase lServiceBase : lServiceBaseList) {
				if(lServiceBase instanceof IService && pClass.isAssignableFrom(lServiceBase.getClass())) {
					lService = (IService) lServiceBase;
					break;
				}
			}
		}
		return lService;
	}

	/**
	 *
	 * @param pArgs
	 *
	 */
	public static void main(String[] pArgs) {
		// TODO Auto-generated method stub
		new Throwable("not yet implemented").printStackTrace();
		
	}
}
