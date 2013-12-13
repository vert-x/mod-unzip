package io.vertx.mods.test.integration;/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.assertNotNull;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

/**
 * Example Java integration test that deploys the module that this project builds.
 * 
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests to start
 * before the module has been deployed.
 * 
 * This test demonstrates how to do that.
 */
public class ModuleIntegrationTest extends TestVerticle {

	private static final String destDir = "src/test/resources/destDir";

	@Test
	public void testUnzipSpecifyDir() {
		JsonObject msg = new JsonObject().putString("zipFile", "src/test/resources/testfile.zip").putString("destDir",
		    destDir);
		vertx.eventBus().send("io.vertx.unzipper", msg, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> reply) {
				assertEquals("ok", reply.body().getString("status"));
				String dest = reply.body().getString("destDir");
				assertEquals(destDir, dest);
				assertUnzipped(dest);
				testComplete();
			}
		});
	}

	@Test
	public void testUnzipTempDir() {
		JsonObject msg = new JsonObject().putString("zipFile", "src/test/resources/testfile.zip");
		vertx.eventBus().send("io.vertx.unzipper", msg, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> reply) {
				assertEquals("ok", reply.body().getString("status"));
				String dest = reply.body().getString("destDir");
				assertNotNull(dest);
				assertUnzipped(dest);
				testComplete();
			}
		});
	}

	private void assertUnzipped(String dest) {
		assertTrue(vertx.fileSystem().existsSync(dest + "/some-dir"));
		assertTrue(vertx.fileSystem().existsSync(dest + "/some-dir/textfile.txt"));
		assertTrue(vertx.fileSystem().existsSync(dest + "/some-dir/some-dir2"));
		assertTrue(vertx.fileSystem().existsSync(dest + "/some-dir/some-dir2/textfile2.txt"));
	}

	@Override
	public void start() {
		cleanup();
		// Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
		initialize();
		// Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
		// don't have to hardecode it in your tests
		container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
			@Override
			public void handle(AsyncResult<String> asyncResult) {
				// Deployment is asynchronous and this this handler will be called when it's complete (or failed)
				assertTrue(asyncResult.succeeded());
				assertNotNull("deploymentID should not be null", asyncResult.result());
				// If deployed correctly then start the tests!
				startTests();
			}
		});
	}

	@Override
	public void stop() {
		cleanup();
	}

	private void cleanup() {
		try {
			vertx.fileSystem().deleteSync(destDir, true);
		} catch (Exception ignore) {
		}
	}

}
