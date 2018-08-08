/**
 * Copyright (C) 2013 codingtony (t.bussieres@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.butor.netty.handler.codec.ftp.impl;

import com.butor.netty.handler.codec.ftp.cmd.AbstractFTPCommand;
import com.butor.netty.handler.codec.ftp.cmd.ActivePassiveSocketManager;
import com.butor.netty.handler.codec.ftp.cmd.FTPAttrKeys;
import com.butor.netty.handler.codec.ftp.cmd.FTPCommand;
import com.butor.netty.handler.codec.ftp.fs.AbstractVirtualFile;
import com.butor.netty.handler.codec.ftp.fs.Permission;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * List
 */
public class ListCmd extends AbstractFTPCommand {

    private final ActivePassiveSocketManager activePassiveSocketManager;

    public ListCmd(ActivePassiveSocketManager activePassiveSocketManager) {
        super("LIST");
        this.activePassiveSocketManager = activePassiveSocketManager;
    }

    @Override
    public void execute(ChannelHandlerContext ctx, String args) {
        Socket activeSocket = activePassiveSocketManager.getActiveSocket(ctx);
        ServerSocket passiveSocket = activePassiveSocketManager.getPassiveSocket(ctx);
        FTPCommand lastFTPCommand = ctx.attr(FTPAttrKeys.LAST_COMMAND).get();
        String lastCommand = lastFTPCommand != null ? lastFTPCommand.getCmd() : null;

        if ("PORT".equals(lastCommand)) {
            if (null == activeSocket) {
                send("503 Bad sequence of commands", ctx, args);
                return;
            }
            send("150 Opening binary mode data connection for LIST " + args,
                    ctx, args);
            try {
                writeListToClient(activeSocket.getOutputStream());
                send("226 Transfer complete for LIST", ctx, args);
            } catch (IOException e1) {
                logger.warn(
                        "Exception thrown on writing through active socket: ["
                                + activeSocket + "]", e1);
                send("552 Requested file action aborted", ctx, args);
            } finally {
                activePassiveSocketManager.closeActiveSocket(ctx);
            }
        } else if ("PASV".equals(lastCommand)) {
            if (null == passiveSocket) {
                send("503 Bad sequence of commands", ctx, args);
                return;
            }

            send("150 Opening binary mode data connection for LIST on port: "
                    + passiveSocket.getLocalPort(), ctx, args);
            Socket clientSocket = null;
            try {
                clientSocket = passiveSocket.accept();
                writeListToClient(clientSocket.getOutputStream());
                clientSocket.getOutputStream().close();
                send("226 Transfer complete for LIST", ctx, args);
            } catch (IOException e1) {
                logger.warn(
                        "Exception thrown on writing through passive socket: ["
                                + passiveSocket + "],"
                                + "accepted client socket: [" + clientSocket
                                + "]", e1);
                send("552 Requested file action aborted", ctx, args);
            } finally {
                activePassiveSocketManager.closePassiveSocket(ctx);
            }
        } else
            send("503 Bad sequence of commands", ctx, args);
    }

    private void writeListToClient(OutputStream outputStream)
            throws IOException {
		AbstractVirtualFile f = new AbstractVirtualFile("test",
				Permission.READWRITE, Permission.WRITE, Permission.EXEC);
		outputStream.write(f.getListString().getBytes(CharsetUtil.UTF_8));
		outputStream.write("drwxrwxr-x   2 0     5     512 Nov 27  2012 .snap"
				.getBytes());
        outputStream.write(CRLF);

    }

}
