/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifos.sdk.group;

import org.mifos.sdk.MifosXConnectException;
import org.mifos.sdk.MifosXResourceException;
import org.mifos.sdk.group.domain.Group;
import org.mifos.sdk.group.domain.GroupAccountsSummary;
import org.mifos.sdk.group.domain.PageableGroups;
import org.mifos.sdk.group.domain.commands.ActivateGroupCommand;
import org.mifos.sdk.group.domain.commands.AssignUnassignStaffCommand;
import org.mifos.sdk.group.domain.commands.AssignUpdateRoleCommand;
import org.mifos.sdk.group.domain.commands.AssociateDisassociateClientsCommand;
import org.mifos.sdk.group.domain.commands.CloseGroupCommand;
import org.mifos.sdk.group.domain.commands.GenerateCollectionSheetCommand;
import org.mifos.sdk.group.domain.commands.SaveCollectionSheetCommand;
import org.mifos.sdk.group.domain.commands.TransferClientsCommand;

import java.util.List;
import java.util.Map;

/**
 * Interface to communicate with the Groups API.
 */
public interface GroupService {

    /**
     * Creates a new group.
     * @param group the {@link Group} to create
     * @return a {@link Group} with the response parameters
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    Group createGroup(final Group group) throws MifosXConnectException, MifosXResourceException;

    /**
     * Retrieves all available groups.
     * @param queryMap a {@link Map} with all the query parameters
     * @return a {@link PageableGroups} with the list of {@link Group}s
     * @throws MifosXConnectException
     */
    PageableGroups fetchGroups(final Map<String, Object> queryMap) throws MifosXConnectException;

    /**
     * Retrieves oe particular group.
     * @param groupId the group ID
     * @param queryMap a {@link Map} with all the query parameters
     * @return the {@link Group} with all the details of the searched group
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    Group findGroup(final Long groupId, final Map<String, Object> queryMap) throws MifosXConnectException,
        MifosXResourceException;

    /**
     * Retrieves the accounts summary of a group.
     * @param groupId the group ID
     * @param fields a {@link List} of fields to include in the response
     * @return the {@link Group} with its accounts summary
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    GroupAccountsSummary findGroupsAccountsSummary(final Long groupId, final List<String> fields) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Updates a particular group.
     * @param groupId the group ID
     * @param group the {@link Group} object with all the changes to be made
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void updateGroup(final Long groupId, final Group group) throws MifosXConnectException,
        MifosXResourceException;

    /**
     * Deletes a particular group.
     * @param groupId the group ID
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void deleteGroup(final Long groupId) throws MifosXConnectException, MifosXResourceException;

    /**
     * Activates a pending group or results in an error if the group is already activated.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.ActivateGroupCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void activateGroup(final Long groupId, final ActivateGroupCommand command) throws MifosXConnectException,
        MifosXResourceException;

    /**
     * Associates clients with a group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.AssociateDisassociateClientsCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void associateClients(final Long groupId, final AssociateDisassociateClientsCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Disassociates clients from a group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.AssociateDisassociateClientsCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void disassociateClients(final Long groupId, final AssociateDisassociateClientsCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Transfers clients from a group to another.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.TransferClientsCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void transferClients(final Long groupId, final TransferClientsCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Generates the collection sheet for the group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.GenerateCollectionSheetCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void generateCollectionSheet(final Long groupId, final GenerateCollectionSheetCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Saves the collection sheet of a group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.SaveCollectionSheetCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void saveCollectionSheet(final Long groupId, final SaveCollectionSheetCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Un-assigns staff from a group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.AssignUnassignStaffCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void unassignStaff(final Long groupId, final AssignUnassignStaffCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Assigns staff to a group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.AssignUnassignStaffCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void assignStaff(final Long groupId, final AssignUnassignStaffCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Closes a group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.CloseGroupCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void closeGroup(final Long groupId, final CloseGroupCommand command) throws MifosXConnectException,
        MifosXResourceException;

    /**
     * Assigns a role to a group.
     * @param groupId the group ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.AssignUpdateRoleCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void assignRole(final Long groupId, final AssignUpdateRoleCommand command) throws
        MifosXConnectException, MifosXResourceException;

    /**
     * Un-assigns a role from a group.
     * @param groupId the group ID
     * @param roleId the role ID
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void unassignRole(final Long groupId, final Long roleId) throws MifosXConnectException,
        MifosXResourceException;

    /**
     * Updates an existing role of a group.
     * @param groupId the group ID
     * @param roleId the role ID
     * @param command the {@link org.mifos.sdk.group.domain.commands.AssignUpdateRoleCommand}
     * @throws MifosXConnectException
     * @throws MifosXResourceException
     */
    void updateRole(final Long groupId, final Long roleId, final AssignUpdateRoleCommand command) throws
        MifosXConnectException, MifosXResourceException;

}
