package io.vertx.mods;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManagerException;
import org.vertx.java.platform.Verticle;

import java.io.*;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
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
public class UnzipVerticle extends Verticle {

  private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
  private static final String FILE_SEP = System.getProperty("file.separator");
  private static final int BUFFER_SIZE = 4096;

  @Override
  public void start() {

    JsonObject conf = container.config();
    String address = conf.getString("address", "io.vertx.unzipper");

    vertx.eventBus().registerHandler(address, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        String zipFile = message.body().getString("zipFile");
        if (zipFile == null) {
          sendError("Please specify zipFile field in message", message);
          return;
        }
        String destDir = message.body().getString("destDir");
        if (destDir == null) {
          // Generate a tmp dest dir
          destDir = generateTmpFileName();
        }
        File dest = new File(destDir);
        if (!dest.exists()) {
          if (!dest.mkdir()) {
            sendError("Failed to create directory " + dest, message);
            return;
          }
        }
        boolean deleteZip = message.body().getBoolean("deleteZip", false);
        try {
          unzipModuleData(dest, zipFile);
          if (deleteZip) {
            if (!new File(zipFile).delete()) {
              sendError("Failed to delete zip file " + dest, message);
              return;
            }
          }
          message.reply(new JsonObject().putString("status", "ok").putString("destDir", destDir));
        } catch (Exception e) {
          sendError("Failed to unzip module: " + e.getMessage(), message);
        }
      }
    });
  }

  private void sendError(String errMsg, Message<JsonObject> msg) {
    JsonObject reply = new JsonObject().putString("status", "error").putString("message", errMsg);
    msg.reply(reply);
  }

  private String generateTmpFileName() {
    return TEMP_DIR + FILE_SEP + "vertx-" + UUID.randomUUID().toString();
  }

  private void unzipModuleData(final File directory, final String zipFileName) throws Exception {
    try (InputStream is = new BufferedInputStream(new FileInputStream(zipFileName));
         ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String entryName = entry.getName();
        if (!entryName.isEmpty()) {
          if (entry.isDirectory()) {
            if (!new File(directory, entryName).mkdir()) {
              throw new PlatformManagerException("Failed to create directory");
            }
          } else {
            int count;
            byte[] buff = new byte[BUFFER_SIZE];
            BufferedOutputStream dest = null;
            try {
              OutputStream fos = new FileOutputStream(new File(directory, entryName));
              dest = new BufferedOutputStream(fos, BUFFER_SIZE);
              while ((count = zis.read(buff, 0, BUFFER_SIZE)) != -1) {
                dest.write(buff, 0, count);
              }
              dest.flush();
            } finally {
              if (dest != null) {
                dest.close();
              }
            }
          }
        }
      }
    }
  }
}
