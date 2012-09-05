package com.netflix.simianarmy.client.vsphere;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;

import com.amazonaws.AmazonServiceException;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/*
 *  Copyright 2012 Immobilienscout GmbH
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
/**
 * Wraps the connection to VSphere and handles the raw service calls.
 *
 * @author ingmar.krusch@immobilienscout24.de
 */
public class VSphereServiceConnection {
    private static final String VIRTUAL_MACHINE_TYPE_NAME = "VirtualMachine";

    /** The username that is used to connect to VSpehere Center. */
    private String username = null;

    /** The password that is used to connect to VSpehere Center. */
    private String password = null;

    /** The url that is used to connect to VSpehere Center. */
    private String url = null;

    /** The ServiceInstance that is used to issue multiple requests to VSpehere Center. */
    private ServiceInstance service = null;

    /**
     * Constructor.
     */
    public VSphereServiceConnection(BasicConfiguration config) {
        this.url = config.getStr("client.vsphere.url");
        this.username = config.getStr("client.vsphere.username");
        this.password = config.getStr("client.vsphere.password");
    }

    /** disconnect from the service if not already disconnected. */
    public void disconnect() {
        if (service != null) {
            service.getServerConnection().logout();
            service = null;
        }
    }

    /** connect to the service if not already connected. */
    public void connect() throws AmazonServiceException {
        try {
            if (service == null) {
                service = new ServiceInstance(new URL(url), username, password, true);
            }
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot connect to VSphere", e);
        } catch (MalformedURLException e) {
            throw new AmazonServiceException("cannot connect to VSphere", e);
        }
    }

    /**
     * Gets the named VirtualMachine.
     */
    public VirtualMachine getVirtualMachineById(String instanceId) throws RemoteException {
        Folder rootFolder = service.getRootFolder();
        InventoryNavigator inventoryNavigator = new InventoryNavigator(rootFolder);
        VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator.searchManagedEntity(
                VIRTUAL_MACHINE_TYPE_NAME, instanceId);

        return virtualMachine;
    }

    /**
     * Return all VirtualMachines from VSpehere Center.
     *
     * @throws AmazonServiceException
     *             If there is any communication error or if no VirtualMachine's are found
     */
    public VirtualMachine[] describeVirtualMachines() throws AmazonServiceException {
        ManagedEntity[] mes = null;

        Folder rootFolder = service.getRootFolder();
        try {
            mes = new InventoryNavigator(rootFolder).searchManagedEntities(VIRTUAL_MACHINE_TYPE_NAME);
        } catch (InvalidProperty e) {
            throw new AmazonServiceException("cannot query VSphere", e);
        } catch (RuntimeFault e) {
            throw new AmazonServiceException("cannot query VSphere", e);
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot query VSphere", e);
        }

        if (mes == null || mes.length == 0) {
            throw new AmazonServiceException(
                      "vsphere returned zero entities of type \""
                      + VIRTUAL_MACHINE_TYPE_NAME + "\""
                  );
        } else {
            return Arrays.copyOf(mes, mes.length, VirtualMachine[].class);
        }
    }
}
