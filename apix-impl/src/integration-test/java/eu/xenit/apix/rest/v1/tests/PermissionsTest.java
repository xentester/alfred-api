package eu.xenit.apix.rest.v1.tests;

import static org.junit.Assert.*;

import eu.xenit.apix.data.NodeRef;
import java.io.IOException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by kenneth on 16.03.16.
 */
public class PermissionsTest extends BaseTest {

    private final static Logger logger = LoggerFactory.getLogger(PermissionsTest.class);

    @Before
    public void setup() {
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

    }

    @Test
    public void testPermissionsGet() throws IOException {
        NodeRef[] nodeRef = init();
        String url = makeNodesUrl(nodeRef[0], "/permissions", "admin", "admin");

        HttpResponse httpResponse = Request.Get(url).execute().returnResponse();
        logger.debug(EntityUtils.toString(httpResponse.getEntity()));
        String result = EntityUtils.toString(httpResponse.getEntity());
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testPermissionsShortGet() throws IOException {
        NodeRef[] nodeRef = init();
        String url = makeNodesUrl(nodeRef[0].getGuid(), "/permissions", "admin", "admin");

        HttpResponse httpResponse = Request.Get(url).execute().returnResponse();
        logger.debug(EntityUtils.toString(httpResponse.getEntity()));
        String result = EntityUtils.toString(httpResponse.getEntity());
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());
    }

    /**
     * Read default permission of a node (without applying anything).
     */
    @Test
    public void testNodePermissionGetDefault() throws IOException, JSONException {
        NodeRef[] nodeRef = init();
        String url = makeNodesUrl(nodeRef[0].getGuid(), "/acl", "admin", "admin");

        checkNoPermissionOnNode(url);
    }

    private void checkNoPermissionOnNode(String url) throws IOException, JSONException {
        HttpResponse httpResponse = Request.Get(url).execute().returnResponse();
        logger.debug(EntityUtils.toString(httpResponse.getEntity()));
        String response = EntityUtils.toString(httpResponse.getEntity());

        assertEquals(200, httpResponse.getStatusLine().getStatusCode());

        JSONObject jsonObject = new JSONObject(response);
        assertTrue(jsonObject.has("inheritFromParent"));
        assertTrue(jsonObject.has("ownAccessList"));

        assertEquals(true, jsonObject.getBoolean("inheritFromParent"));

        JSONArray list = jsonObject.getJSONArray("inheritedAccessList");
        assertEquals(1, list.length());

        JSONObject access1 = list.getJSONObject(0);
        assertTrue(access1.has("allowed"));
        assertTrue(access1.has("authority"));
        assertTrue(access1.has("permission"));

        assertEquals(true, access1.getBoolean("allowed"));
        assertEquals("GROUP_EVERYONE", access1.getString("authority"));
        assertEquals("Consumer", access1.getString("permission"));
    }


    /**
     * Apply some permissions and then read back.
     */
    @Test
    public void testNodePermissionApplyGetAndRemove() throws IOException, JSONException {
        NodeRef[] nodeRef = init();
        String url = makeNodesUrl(nodeRef[0].getGuid(), "/acl", "admin", "admin");

        doPut(url,
                null,
                "{\"inheritFromParent\": false, \"ownAccessList\": [{\"allowed\": true, \"authority\": \"%s\", \"permission\": \"%s\"}, {\"allowed\": true, \"authority\": \"%s\", \"permission\": \"%s\"}]}",
                "mjackson", "Collaborator", "abeecher", "Consumer");

        HttpResponse httpResponse = Request.Get(url).execute().returnResponse();
        logger.debug(EntityUtils.toString(httpResponse.getEntity()));
        String response = EntityUtils.toString(httpResponse.getEntity());

        assertEquals(200, httpResponse.getStatusLine().getStatusCode());

        JSONObject jsonObject = new JSONObject(response);
        assertTrue(jsonObject.has("inheritFromParent"));
        assertTrue(jsonObject.has("ownAccessList"));

        assertEquals(false, jsonObject.getBoolean("inheritFromParent"));

        JSONArray list = jsonObject.getJSONArray("ownAccessList");
        assertEquals(2, list.length());

        for (int i = 0; i < list.length(); i++) {
            JSONObject access = list.getJSONObject(0);
            assertTrue(access.has("allowed"));
            assertTrue(access.has("authority"));
            assertTrue(access.has("permission"));
            assertEquals(true, access.getBoolean("allowed"));
            switch (access.getString("authority")) {
                case "abeecher":
                    assertEquals("Consumer", access.getString("permission"));
                    break;
                case "mjackson":
                    assertEquals("Collaborator", access.getString("permission"));
                    break;
                default:
                    fail("authority should be either 'mjackson' or 'abeecher'");
            }
        }

        // Remove permissions
        doPut(url,
                null,
                "{\"inheritFromParent\":true, \"ownAccessList\": []}");

        // Check removed.
        checkNoPermissionOnNode(url);

    }


    @After
    public void cleanUp() {
        this.removeMainTestFolder();
    }
}
