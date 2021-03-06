/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifos.sdk.client.internal;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mifos.sdk.MifosXConnectException;
import org.mifos.sdk.MifosXProperties;
import org.mifos.sdk.MifosXResourceException;
import org.mifos.sdk.client.domain.Client;
import org.mifos.sdk.client.domain.ClientIdentifier;
import org.mifos.sdk.client.domain.ClientImage;
import org.mifos.sdk.client.domain.PageableClients;
import org.mifos.sdk.client.domain.commands.*;
import org.mifos.sdk.internal.ErrorCode;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.mime.TypedString;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Test for {@link RestClientService} and its various methods.
 */
public class RestClientServiceTest {

    private RetrofitClientService retrofitClientService;
    private MifosXProperties properties;
    private String mockedAuthKey;
    private Client defaultClient;
    private Long defaultClientId;
    private RestClientService clientService;
    private String defaultDuplicateJSON;
    private String defaultDuplicateMessage;
    private ClientIdentifier clientIdentifier;
    private ClientImage clientImage;
    private Long defaultIdentifierId;
    private BufferedImage defaultImage;
    private TypedString defaultBase64Data;

    /**
     * Setup all the components before testing.
     */
    @Before
    public void setup() throws IOException {
        final RestAdapter restAdapter = mock(RestAdapter.class);
        this.retrofitClientService = mock(RetrofitClientService.class);
        this.properties = MifosXProperties
                .url("http://demo.openmf.org/mifosng-provider/api/v1")
                .username("mifos")
                .password("password")
                .tenant("default")
                .build();
        this.mockedAuthKey = "=hd$$34dd";
        this.defaultClient = Client
                .fullname("Davis Jones")
                .officeId(1L)
                .active(false)
                .build();
        this.defaultClientId = 1L;
        this.defaultClient.setClientId(this.defaultClientId);
        this.clientService = new RestClientService(this.properties, restAdapter,
                this.mockedAuthKey);
        this.mockedAuthKey = "Basic " + this.mockedAuthKey;
        this.defaultDuplicateJSON = "{\"errors\":[{\"developerMessage\":\"some random message\"}]}";
        this.defaultDuplicateMessage = "some random message";
        this.clientIdentifier = ClientIdentifier.documentKey("dummy").build();
        this.defaultIdentifierId = 1L;
        this.clientIdentifier.setClientId(this.defaultClientId);
        this.defaultImage = ImageIO.read(RestClientServiceTest.class.getResource("/profile_pic.png"));

        // generate the Data URI for the default image
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(this.defaultImage, "png", outputStream);
        final byte[] binaryData = outputStream.toByteArray();
        this.defaultBase64Data = new TypedString("data:image/png;base64," + Base64.encodeBase64String(binaryData));

        this.clientImage = ClientImage.image(this.defaultImage).type(ClientImage.Type.PNG).build();
        this.clientImage.setResourceId(this.defaultClientId);

        when(restAdapter.create(RetrofitClientService.class)).thenReturn(this.retrofitClientService);
    }

    /**
     * Test for successful creation of a client.
     */
    @Test
    public void testCreateClient() {
        when(this.retrofitClientService.createClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClient)).thenReturn(this.defaultClient);

        try {
            final Long id = this.clientService.createClient(this.defaultClient).getClientId();

            Assert.assertNotNull(id);
            Assert.assertEquals(id, this.defaultClientId);
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link org.mifos.sdk.internal.ErrorCode#NOT_CONNECTED} exception for createClient().
     */
    @Test
    public void testCreateClientNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        when(this.retrofitClientService.createClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClient)).thenThrow(error);

        try {
            this.clientService.createClient(this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for createClient().
     */
    @Test
    public void testCreateClientDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(),
                new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.createClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClient)).thenThrow(error);

        try {
            this.clientService.createClient(this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for conversion error exception for createClient().
     */
    @Test
    public void testCreateClientConversionException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.CONVERSION);
        when(this.retrofitClientService.createClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClient)).thenThrow(error);

        try {
            this.clientService.createClient(this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for createClient().
     */
    @Test
    public void testCreateClientInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.createClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClient)).thenThrow(error);

        try {
            this.clientService.createClient(this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for createClient().
     */
    @Test
    public void testCreateClientUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.createClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClient)).thenThrow(error);

        try {
            this.clientService.createClient(this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test to fetch all available clients.
     */
    @Test
    public void testFetchClients() {
        final List<Client> clientsList = Arrays.asList(this.defaultClient, this.defaultClient);
        final PageableClients pageableClients = new PageableClients();
        pageableClients.setClients(clientsList);

        when(this.retrofitClientService.fetchClients(this.mockedAuthKey,
                this.properties.getTenant(), null)).thenReturn(pageableClients);

        try {
            final List<Client> responseClients = this.clientService.fetchClients(null).getClients();

            Assert.assertNotNull(responseClients);
            Assert.assertThat(responseClients, equalTo(clientsList));
        } catch (MifosXConnectException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for fetchClients().
     */
    @Test
    public void testFetchClientsNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        when(this.retrofitClientService.fetchClients(this.mockedAuthKey,
                this.properties.getTenant(), null)).thenThrow(error);

        try {
            this.clientService.fetchClients(null);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        }
    }

    /**
     * Test for conversion error exception for fetchClients().
     */
    @Test
    public void testFetchClientsConversionException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.CONVERSION);
        when(this.retrofitClientService.fetchClients(this.mockedAuthKey,
                this.properties.getTenant(), null)).thenThrow(error);

        try {
            this.clientService.fetchClients(null);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for fetchClients().
     */
    @Test
    public void testFetchClientsInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.fetchClients(this.mockedAuthKey,
                this.properties.getTenant(), null)).thenThrow(error);

        try {
            this.clientService.fetchClients(null);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for fetchClients().
     */
    @Test
    public void testFetchClientsUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.fetchClients(this.mockedAuthKey,
                this.properties.getTenant(), null)).thenThrow(error);

        try {
            this.clientService.fetchClients(null);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        }
    }

    /**
     * Test to find one particular client.
     */
    @Test
    public void testFindClient() {
        when(this.retrofitClientService.findClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClientId)).thenReturn(this.defaultClient);

        try {
            final Client responseClient = this.clientService.findClient(this.defaultClientId);

            Assert.assertNotNull(responseClient);
            Assert.assertThat(responseClient, equalTo(this.defaultClient));
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for findClient().
     */
    @Test
    public void testFindClientNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        when(this.retrofitClientService.findClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.findClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for findClient().
     */
    @Test
    public void testFindClientDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.findClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for conversion error exception for findClient().
     */
    @Test
    public void testFindClientConversionException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.CONVERSION);
        when(this.retrofitClientService.findClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.findClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for findClient().
     */
    @Test
    public void testFindClientNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.findClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch(MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for findClient().
     */
    @Test
    public void testFindClientInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.findClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch(MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for findClient().
     */
    @Test
    public void testFindClientUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findClient(this.mockedAuthKey, this.properties.getTenant(),
                this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.findClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for updateClient().
     */
    @Test
    public void testUpdateClientNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).updateClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, this.defaultClient);

        try {
            this.clientService.updateClient(this.defaultClientId, this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for updateClient().
     */
    @Test
    public void testUpdateClientDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, this.defaultClient);

        try {
            this.clientService.updateClient(this.defaultClientId, this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for updateClient().
     */
    @Test
    public void testUpdateClientInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, this.defaultClient);

        try {
            this.clientService.updateClient(this.defaultClientId, this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for updateClient().
     */
    @Test
    public void testUpdateClientNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, this.defaultClient);

        try {
            this.clientService.updateClient(this.defaultClientId, this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for updateClient().
     */
    @Test
    public void testUpdateClientUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, this.defaultClient);

        try {
            this.clientService.updateClient(this.defaultClientId, this.defaultClient);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for deleteClient().
     */
    @Test
    public void testDeleteClientNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).deleteClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for deleteClient().
     */
    @Test
    public void testDeleteClientDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for deleteClient().
     */
    @Test
    public void testDeleteClientInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for deleteClient().
     */
    @Test
    public void testDeleteClientNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for deleteClient().
     */
    @Test
    public void testDeleteClientUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteClient(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteClient(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for activateClient().
     */
    @Test
    public void testActivateClientNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final ActivateClientCommand command = ActivateClientCommand.locale("en").build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "activate", command);

        try {
            this.clientService.activateClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for activateClient().
     */
    @Test
    public void testActivateClientDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final ActivateClientCommand command = ActivateClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "activate", command);

        try {
            this.clientService.activateClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for activateClient().
     */
    @Test
    public void testActivateClientInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final ActivateClientCommand command = ActivateClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "activate", command);

        try {
            this.clientService.activateClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for activateClient().
     */
    @Test
    public void testActivateClientNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final ActivateClientCommand command = ActivateClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "activate", command);

        try {
            this.clientService.activateClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for activateClient().
     */
    @Test
    public void testActivateClientUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final ActivateClientCommand command = ActivateClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "activate", command);

        try {
            this.clientService.activateClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for closeClient().
     */
    @Test
    public void testCloseClientNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final CloseClientCommand command = CloseClientCommand.locale("en").build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "close", command);

        try {
            this.clientService.closeClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for closeClient().
     */
    @Test
    public void testCloseClientDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final CloseClientCommand command = CloseClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "close", command);

        try {
            this.clientService.closeClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for closeClient().
     */
    @Test
    public void testCloseClientInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final CloseClientCommand command = CloseClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "close", command);

        try {
            this.clientService.closeClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for closeClient().
     */
    @Test
    public void testCloseClientNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final CloseClientCommand command = CloseClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "close", command);

        try {
            this.clientService.closeClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for closeClient().
     */
    @Test
    public void testCloseClientUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final CloseClientCommand command = CloseClientCommand.locale("en").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "close", command);

        try {
            this.clientService.closeClient(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for assignStaff().
     */
    @Test
    public void testAssignStaffNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "assignStaff", command);

        try {
            this.clientService.assignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for assignStaff().
     */
    @Test
    public void testAssignStaffDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "assignStaff", command);

        try {
            this.clientService.assignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for assignStaff().
     */
    @Test
    public void testAssignStaffInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "assignStaff", command);

        try {
            this.clientService.assignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for assignStaff().
     */
    @Test
    public void testAssignStaffNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "assignStaff", command);

        try {
            this.clientService.assignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for assignStaff().
     */
    @Test
    public void testAssignStaffUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "assignStaff", command);

        try {
            this.clientService.assignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for unassignStaff().
     */
    @Test
    public void testUnassignStaffNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "unassignStaff", command);

        try {
            this.clientService.unassignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for unassignStaff().
     */
    @Test
    public void testUnassignStaffDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "unassignStaff", command);

        try {
            this.clientService.unassignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for unassignStaff().
     */
    @Test
    public void testUnassignStaffInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "unassignStaff", command);

        try {
            this.clientService.unassignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for unassignStaff().
     */
    @Test
    public void testUnassignStaffNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "unassignStaff", command);

        try {
            this.clientService.unassignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for unassignStaff().
     */
    @Test
    public void testUnassignStaffUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final AssignUnassignStaffCommand command = AssignUnassignStaffCommand.staffId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "unassignStaff", command);

        try {
            this.clientService.unassignStaff(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for updateSavingsAccount().
     */
    @Test
    public void testUpdateSavingsAccountNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final UpdateSavingsAccountCommand command = UpdateSavingsAccountCommand.savingsAccountId(1L).build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "updateSavingsAccount", command);

        try {
            this.clientService.updateSavingsAccount(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for updateSavingsAccount().
     */
    @Test
    public void testUpdateSavingsAccountDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final UpdateSavingsAccountCommand command = UpdateSavingsAccountCommand.savingsAccountId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "updateSavingsAccount", command);

        try {
            this.clientService.updateSavingsAccount(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for updateSavingsAccount().
     */
    @Test
    public void testUpdateSavingsAccountInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final UpdateSavingsAccountCommand command = UpdateSavingsAccountCommand.savingsAccountId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "updateSavingsAccount", command);

        try {
            this.clientService.updateSavingsAccount(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for updateSavingsAccount().
     */
    @Test
    public void testUpdateSavingsAccountNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final UpdateSavingsAccountCommand command = UpdateSavingsAccountCommand.savingsAccountId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "updateSavingsAccount", command);

        try {
            this.clientService.updateSavingsAccount(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for updateSavingsAccount().
     */
    @Test
    public void testUpdateSavingsAccountUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final UpdateSavingsAccountCommand command = UpdateSavingsAccountCommand.savingsAccountId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "updateSavingsAccount", command);

        try {
            this.clientService.updateSavingsAccount(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for proposeTransfer().
     */
    @Test
    public void testProposeTransferNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final ProposeClientTransferCommand command = ProposeClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeTransfer", command);

        try {
            this.clientService.proposeTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for proposeTransfer().
     */
    @Test
    public void testProposeTransferDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final ProposeClientTransferCommand command = ProposeClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeTransfer", command);

        try {
            this.clientService.proposeTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for proposeTransfer().
     */
    @Test
    public void testProposeTransferInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final ProposeClientTransferCommand command = ProposeClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeTransfer", command);

        try {
            this.clientService.proposeTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for proposeTransfer().
     */
    @Test
    public void testProposeTransferNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final ProposeClientTransferCommand command = ProposeClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeTransfer", command);

        try {
            this.clientService.proposeTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for proposeTransfer().
     */
    @Test
    public void testProposeTransferUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final ProposeClientTransferCommand command = ProposeClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeTransfer", command);

        try {
            this.clientService.proposeTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for withdrawTransfer().
     */
    @Test
    public void testWithdrawTransferNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "withdrawTransfer", command);

        try {
            this.clientService.withdrawTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for withdrawTransfer().
     */
    @Test
    public void testWithdrawTransferDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "withdrawTransfer", command);

        try {
            this.clientService.withdrawTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for withdrawTransfer().
     */
    @Test
    public void testWithdrawTransferInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "withdrawTransfer", command);

        try {
            this.clientService.withdrawTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for withdrawTransfer().
     */
    @Test
    public void testWithdrawTransferNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "withdrawTransfer", command);

        try {
            this.clientService.withdrawTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for withdrawTransfer().
     */
    @Test
    public void testWithdrawTransferUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "withdrawTransfer", command);

        try {
            this.clientService.withdrawTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for rejectTransfer().
     */
    @Test
    public void testRejectTransferNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "rejectTransfer", command);

        try {
            this.clientService.rejectTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for rejectTransfer().
     */
    @Test
    public void testRejectTransferDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "rejectTransfer", command);

        try {
            this.clientService.rejectTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for rejectTransfer().
     */
    @Test
    public void testRejectTransferInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "rejectTransfer", command);

        try {
            this.clientService.rejectTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for rejectTransfer().
     */
    @Test
    public void testRejectTransferNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "rejectTransfer", command);

        try {
            this.clientService.rejectTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for rejectTransfer().
     */
    @Test
    public void testRejectTransferUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final WithdrawRejectClientTransferCommand command = WithdrawRejectClientTransferCommand.note("dummy").build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "rejectTransfer", command);

        try {
            this.clientService.rejectTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for acceptTransfer().
     */
    @Test
    public void testAcceptTransferNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final AcceptClientTransferCommand command = AcceptClientTransferCommand.destinationGroupId(1L).build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "acceptTransfer", command);

        try {
            this.clientService.acceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for acceptTransfer().
     */
    @Test
    public void testAcceptTransferDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final AcceptClientTransferCommand command = AcceptClientTransferCommand.destinationGroupId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "acceptTransfer", command);

        try {
            this.clientService.acceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for acceptTransfer().
     */
    @Test
    public void testAcceptTransferInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final AcceptClientTransferCommand command = AcceptClientTransferCommand.destinationGroupId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "acceptTransfer", command);

        try {
            this.clientService.acceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for acceptTransfer().
     */
    @Test
    public void testAcceptTransferNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final AcceptClientTransferCommand command = AcceptClientTransferCommand.destinationGroupId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "acceptTransfer", command);

        try {
            this.clientService.acceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for acceptTransfer().
     */
    @Test
    public void testAcceptTransferUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final AcceptClientTransferCommand command = AcceptClientTransferCommand.destinationGroupId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "acceptTransfer", command);

        try {
            this.clientService.acceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for proposeAndAcceptTransfer().
     */
    @Test
    public void testProposeAndAcceptTransferNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);
        final ProposeAndAcceptClientTransferCommand command = ProposeAndAcceptClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeAndAcceptTransfer", command);

        try {
            this.clientService.proposeAndAcceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for proposeAndAcceptTransfer().
     */
    @Test
    public void testProposeAndAcceptTransferDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));
        final ProposeAndAcceptClientTransferCommand command = ProposeAndAcceptClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeAndAcceptTransfer", command);

        try {
            this.clientService.proposeAndAcceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for proposeAndAcceptTransfer().
     */
    @Test
    public void testProposeAndAcceptTransferInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));
        final ProposeAndAcceptClientTransferCommand command = ProposeAndAcceptClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeAndAcceptTransfer", command);

        try {
            this.clientService.proposeAndAcceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for proposeAndAcceptTransfer().
     */
    @Test
    public void testProposeAndAcceptTransferNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));
        final ProposeAndAcceptClientTransferCommand command = ProposeAndAcceptClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeAndAcceptTransfer", command);

        try {
            this.clientService.proposeAndAcceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for proposeAndAcceptTransfer().
     */
    @Test
    public void testProposeAndAcceptTransferUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));
        final ProposeAndAcceptClientTransferCommand command = ProposeAndAcceptClientTransferCommand.destinationOfficeId(1L).build();

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).executeCommand(this.mockedAuthKey,
                this.properties.getTenant(), this.defaultClientId, "proposeAndAcceptTransfer", command);

        try {
            this.clientService.proposeAndAcceptTransfer(this.defaultClientId, command);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for successful creation of an identifier.
     */
    @Test
    public void testCreateIdentifier() {
        when(this.retrofitClientService.createIdentifier(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.clientIdentifier)).thenReturn(this.clientIdentifier);

        try {
            final Long id = this.clientService.createIdentifier(this.defaultClientId,
                this.clientIdentifier).getClientId();

            Assert.assertNotNull(id);
            Assert.assertEquals(id, this.defaultClientId);
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link org.mifos.sdk.internal.ErrorCode#NOT_CONNECTED} exception for createIdentifier().
     */
    @Test
    public void testCreateIdentifierNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        when(this.retrofitClientService.createIdentifier(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.clientIdentifier)).thenThrow(error);

        try {
            this.clientService.createIdentifier(this.defaultClientId, this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for createIdentifier().
     */
    @Test
    public void testCreateIdentifierDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(),
            new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.createIdentifier(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.clientIdentifier)).thenThrow(error);

        try {
            this.clientService.createIdentifier(this.defaultClientId, this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for conversion error exception for createIdentifier().
     */
    @Test
    public void testCreateIdentifierConversionException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.CONVERSION);
        when(this.retrofitClientService.createIdentifier(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.clientIdentifier)).thenThrow(error);

        try {
            this.clientService.createIdentifier(this.defaultClientId, this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for createIdentifier().
     */
    @Test
    public void testCreateIdentifierInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.createIdentifier(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.clientIdentifier)).thenThrow(error);

        try {
            this.clientService.createIdentifier(this.defaultClientId, this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for createIdentifier().
     */
    @Test
    public void testCreateIdentifierUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.createIdentifier(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.clientIdentifier)).thenThrow(error);

        try {
            this.clientService.createIdentifier(this.defaultClientId, this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test to fetch all available identifiers.
     */
    @Test
    public void testFetchIdentifiers() {
        final List<ClientIdentifier> identifierList = Arrays.asList(this.clientIdentifier, this.clientIdentifier);

        when(this.retrofitClientService.fetchIdentifiers(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId)).thenReturn(identifierList);

        try {
            final List<ClientIdentifier> responseIdentifiers = this.clientService.fetchIdentifiers(this.defaultClientId);

            Assert.assertNotNull(responseIdentifiers);
            Assert.assertThat(responseIdentifiers, equalTo(identifierList));
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for fetchIdentifiers().
     */
    @Test
    public void testFetchIdentifiersNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        when(this.retrofitClientService.fetchIdentifiers(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.fetchIdentifiers(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for conversion error exception for fetchIdentifiers().
     */
    @Test
    public void testFetchIdentifiersConversionException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.CONVERSION);
        when(this.retrofitClientService.fetchIdentifiers(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.fetchIdentifiers(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for fetchIdentifiers().
     */
    @Test
    public void testFetchIdentifiersInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.fetchIdentifiers(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.fetchIdentifiers(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for fetchIdentifiers().
     */
    @Test
    public void testFetchIdentifiersUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.fetchIdentifiers(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.fetchIdentifiers(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#CLIENT_NOT_FOUND} exception for fetchIdentifiers().
     */
    @Test
    public void testFetchIdentifiersNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.fetchIdentifiers(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId)).thenThrow(error);

        try {
            this.clientService.fetchIdentifiers(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test to find one particular identifier.
     */
    @Test
    public void testFindIdentifier() {
        when(this.retrofitClientService.findIdentifier(this.mockedAuthKey, this.properties
            .getTenant(), this.defaultClientId, this.defaultIdentifierId)).thenReturn(this.clientIdentifier);

        try {
            final ClientIdentifier responseIdentifier = this.clientService.findIdentifier(this.defaultClientId,
                this.defaultIdentifierId);

            Assert.assertNotNull(responseIdentifier);
            Assert.assertThat(responseIdentifier, equalTo(this.clientIdentifier));
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for findIdentifier().
     */
    @Test
    public void testFindIdentifierNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        when(this.retrofitClientService.findIdentifier(this.mockedAuthKey, this.properties
            .getTenant(), this.defaultClientId, this.defaultIdentifierId)).thenThrow(error);

        try {
            this.clientService.findIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for findIdentifier().
     */
    @Test
    public void testFindIdentifierDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findIdentifier(this.mockedAuthKey, this.properties
            .getTenant(), this.defaultClientId, this.defaultIdentifierId)).thenThrow(error);

        try {
            this.clientService.findIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for conversion error exception for findIdentifier().
     */
    @Test
    public void testFindIdentifierConversionException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.CONVERSION);
        when(this.retrofitClientService.findIdentifier(this.mockedAuthKey, this.properties
            .getTenant(), this.defaultClientId, this.defaultIdentifierId)).thenThrow(error);

        try {
            this.clientService.findIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for findIdentifier().
     */
    @Test
    public void testFindIdentifierNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findIdentifier(this.mockedAuthKey, this.properties
            .getTenant(), this.defaultClientId, this.defaultIdentifierId)).thenThrow(error);

        try {
            this.clientService.findIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch(MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_OR_IDENTIFIER_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for findIdentifier().
     */
    @Test
    public void testFindIdentifierInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findIdentifier(this.mockedAuthKey, this.properties
            .getTenant(), this.defaultClientId, this.defaultIdentifierId)).thenThrow(error);

        try {
            this.clientService.findIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch(MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for findIdentifier().
     */
    @Test
    public void testFindIdentifierUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.findIdentifier(this.mockedAuthKey, this.properties
            .getTenant(), this.defaultClientId, this.defaultIdentifierId)).thenThrow(error);

        try {
            this.clientService.findIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for updateIdentifier().
     */
    @Test
    public void testUpdateIdentifierNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).updateIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId,
            this.clientIdentifier);

        try {
            this.clientService.updateIdentifier(this.defaultClientId, this.defaultIdentifierId,
                this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for updateIdentifier().
     */
    @Test
    public void testUpdateIdentifierDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId,
            this.clientIdentifier);

        try {
            this.clientService.updateIdentifier(this.defaultClientId, this.defaultIdentifierId,
                this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for updateIdentifier().
     */
    @Test
    public void testUpdateIdentifierInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId,
            this.clientIdentifier);

        try {
            this.clientService.updateIdentifier(this.defaultClientId, this.defaultIdentifierId,
                this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for updateIdentifier().
     */
    @Test
    public void testUpdateIdentifierNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId,
            this.clientIdentifier);

        try {
            this.clientService.updateIdentifier(this.defaultClientId, this.defaultIdentifierId,
                this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_OR_IDENTIFIER_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for updateIdentifier().
     */
    @Test
    public void testUpdateIdentifierUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId,
            this.clientIdentifier);

        try {
            this.clientService.updateIdentifier(this.defaultClientId, this.defaultIdentifierId,
                this.clientIdentifier);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for deleteIdentifier().
     */
    @Test
    public void testDeleteIdentifierNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).deleteIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId);


        try {
            this.clientService.deleteIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for deleteIdentifier().
     */
    @Test
    public void testDeleteIdentifierDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId);

        try {
            this.clientService.deleteIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for deleteIdentifier().
     */
    @Test
    public void testDeleteIdentifierInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId);

        try {
            this.clientService.deleteIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for deleteIdentifier().
     */
    @Test
    public void testDeleteIdentifierNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId);

        try {
            this.clientService.deleteIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_OR_IDENTIFIER_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for deleteIdentifier().
     */
    @Test
    public void testDeleteIdentifierUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteIdentifier(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultIdentifierId);

        try {
            this.clientService.deleteIdentifier(this.defaultClientId, this.defaultIdentifierId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for successful upload of a client image.
     */
    @Test
    public void testUploadClientImage() {
        when(this.retrofitClientService.uploadImage(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.defaultBase64Data)).thenReturn(this.clientImage);

        try {
            Long id = this.clientService.uploadImage(this.defaultClientId, this.clientImage).getResourceId();

            Assert.assertNotNull(id);
            Assert.assertEquals(id, this.defaultClientId);
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link org.mifos.sdk.internal.ErrorCode#NOT_CONNECTED} exception for uploadImage().
     */
    @Test
    public void testUploadClientImageNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        when(this.retrofitClientService.uploadImage(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.defaultBase64Data)).thenThrow(error);

        try {
            this.clientService.uploadImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for uploadImage().
     */
    @Test
    public void testUploadClientImageDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(),
            new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.uploadImage(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.defaultBase64Data)).thenThrow(error);

        try {
            this.clientService.uploadImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for conversion error exception for uploadImage().
     */
    @Test
    public void testUploadClientImageConversionException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.CONVERSION);
        when(this.retrofitClientService.uploadImage(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.defaultBase64Data)).thenThrow(error);

        try {
            this.clientService.uploadImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for uploadImage().
     */
    @Test
    public void testUploadClientImageInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.uploadImage(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.defaultBase64Data)).thenThrow(error);

        try {
            this.clientService.uploadImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for uploadImage().
     */
    @Test
    public void testUploadClientImageUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        when(this.retrofitClientService.uploadImage(this.mockedAuthKey, this.properties.getTenant(),
            this.defaultClientId, this.defaultBase64Data)).thenThrow(error);

        try {
            this.clientService.uploadImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for updateImage().
     */
    @Test
    public void testUpdateClientImageNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).updateImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultBase64Data);

        try {
            this.clientService.updateImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for updateImage().
     */
    @Test
    public void testUpdateClientImageDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultBase64Data);

        try {
            this.clientService.updateImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for updateImage().
     */
    @Test
    public void testUpdateClientImageInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultBase64Data);

        try {
            this.clientService.updateImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for updateImage().
     */
    @Test
    public void testUpdateClientImageNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultBase64Data);

        try {
            this.clientService.updateImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for updateImage().
     */
    @Test
    public void testUpdateClientImageUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).updateImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId, this.defaultBase64Data);

        try {
            this.clientService.updateImage(this.defaultClientId, this.clientImage);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for {@link ErrorCode#NOT_CONNECTED} exception for deleteImage().
     */
    @Test
    public void testDeleteClientImageNotConnectedException() {
        final RetrofitError error = mock(RetrofitError.class);

        when(error.getKind()).thenReturn(RetrofitError.Kind.NETWORK);
        doThrow(error).when(this.retrofitClientService).deleteImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteImage(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.NOT_CONNECTED.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for duplicate exception for deleteImage().
     */
    @Test
    public void testDeleteClientImageDuplicateException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 403, "", new ArrayList<Header>(), new TypedString(this.defaultDuplicateJSON));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteImage(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), this.defaultDuplicateMessage);
        }
    }

    /**
     * Test for {@link ErrorCode#INVALID_AUTHENTICATION_TOKEN} exception for deleteImage().
     */
    @Test
    public void testDeleteClientImageInvalidAuthKeyException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 401, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteImage(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.INVALID_AUTHENTICATION_TOKEN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

    /**
     * Test for not found exception for deleteImage().
     */
    @Test
    public void testDeleteClientImageNotFoundException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 404, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteImage(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.fail();
        } catch (MifosXResourceException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    /**
     * Test for {@link ErrorCode#UNKNOWN} exception for deleteImage().
     */
    @Test
    public void testDeleteClientImageUnknownException() {
        final RetrofitError error = mock(RetrofitError.class);
        final Response response = new Response("", 503, "", new ArrayList<Header>(), new TypedString(""));

        when(error.getResponse()).thenReturn(response);
        doThrow(error).when(this.retrofitClientService).deleteImage(this.mockedAuthKey,
            this.properties.getTenant(), this.defaultClientId);

        try {
            this.clientService.deleteImage(this.defaultClientId);

            Assert.fail();
        } catch (MifosXConnectException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(e.getMessage(), ErrorCode.UNKNOWN.getMessage());
        } catch (MifosXResourceException e) {
            Assert.fail();
        }
    }

}
