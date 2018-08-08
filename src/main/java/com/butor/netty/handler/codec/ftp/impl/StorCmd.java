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

import com.butor.netty.handler.codec.ftp.DataReceiver;
import com.butor.netty.handler.codec.ftp.cmd.AbstractFTPCommand;
import com.butor.netty.handler.codec.ftp.cmd.ActivePassiveSocketManager;
import com.butor.netty.handler.codec.ftp.cmd.FTPAttrKeys;
import com.butor.netty.handler.codec.ftp.cmd.FTPCommand;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Store File
 */
public class StorCmd extends AbstractFTPCommand {


    private final ActivePassiveSocketManager activePassiveSocketManager;
    private final DataReceiver dataReceiver;

    public StorCmd(ActivePassiveSocketManager activePassiveSocketManager, DataReceiver dataReceiver) {
        super("STOR");
        this.activePassiveSocketManager = activePassiveSocketManager;
        this.dataReceiver = dataReceiver;
    }

    @Override
    public void execute(ChannelHandlerContext ctx, String args) {
        FTPCommand lastFTPCommand = ctx.attr(FTPAttrKeys.LAST_COMMAND).get();
        String lastCommand = lastFTPCommand != null ? lastFTPCommand.getCmd() : null;
        Socket activeSocket = activePassiveSocketManager.getActiveSocket(ctx);
        ServerSocket passiveSocket = activePassiveSocketManager.getPassiveSocket(ctx);

        if ("PORT".equals(lastCommand)) {
            if (null == activeSocket)
                send("503 Bad sequence of commands", ctx, args);
            send("150 Opening binary mode data connection for " + args, ctx, args);
            try {
                dataReceiver.receive(args, activeSocket.getInputStream());
                send("226 Transfer complete for STOR " + args, ctx, args);
            } catch (IOException e1) {
                logger.warn(
                        "Exception thrown on reading through active socket: ["
                                + activeSocket + "]", e1);
                send("552 Requested file action aborted", ctx, args);
            } finally {
                activePassiveSocketManager.closeActiveSocket(ctx);
            }
        } else if ("PASV".equals(lastCommand)) {
            if (null == passiveSocket)
                send("503 Bad sequence of commands", ctx, args);
            send("150 Opening binary mode data connection for " + args, ctx, args);
            Socket clientSocket = null;
            try {
                clientSocket = passiveSocket.accept();
                dataReceiver.receive(args, clientSocket.getInputStream());
                send("226 Transfer complete for STOR " + args, ctx, args);
            } catch (IOException e1) {
                logger.warn(
                        "Exception thrown on reading through passive socket: ["
                                + passiveSocket + "], "
                                + "accepted client socket: [" + clientSocket
                                + "]", e1);
                send("552 Requested file action aborted", ctx, args);
            } finally {
                activePassiveSocketManager.closePassiveSocket(ctx);
            }
        } else
            send("503 Bad sequence of commands", ctx, args);
    }


}
