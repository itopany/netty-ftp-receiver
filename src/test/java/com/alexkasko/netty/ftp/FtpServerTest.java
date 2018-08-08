/**
 * Copyright (C) 2013 codingtony (t.bussieres@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alexkasko.netty.ftp;

import static org.junit.Assert.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;

import com.butor.netty.handler.codec.ftp.CrlfStringDecoder;
import com.butor.netty.handler.codec.ftp.FtpServerHandler;
import com.butor.netty.handler.codec.ftp.cmd.DefaultCommandExecutionTemplate;

/**
 * User: alexkasko
 * Date: 12/28/12
 */
public class FtpServerTest {

    @Test
    public void test() throws IOException, InterruptedException {
    	final DefaultCommandExecutionTemplate defaultCommandExecutionTemplate = new DefaultCommandExecutionTemplate(new ConsoleReceiver());
    	EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
    	ServerBootstrap b = new ServerBootstrap();
    	b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipe = ch.pipeline();
			            pipe.addLast("decoder", new CrlfStringDecoder());
			            pipe.addLast("handler", new FtpServerHandler(defaultCommandExecutionTemplate));
					}
				
				});
    	b.localAddress(2121).bind();
        FTPClient client = new FTPClient();
//        https://issues.apache.org/jira/browse/NET-493
        
        client.setBufferSize(0);
        client.connect("127.0.0.1", 2121);
        assertEquals(230,client.user("anonymous"));
        
        // active
        assertTrue(client.setFileType(FTP.BINARY_FILE_TYPE));
        assertEquals("/",client.printWorkingDirectory());
        assertTrue(client.changeWorkingDirectory("/foo"));
        assertEquals("/foo",client.printWorkingDirectory());
        FTPFile[] ftpFiles = client.listFiles("/foo");
        System.out.println(Arrays.toString(ftpFiles));
        assertTrue(ftpFiles.length==2);
        assertTrue(client.storeFile("bar", new ByteArrayInputStream("content".getBytes())));
        assertTrue(client.rename("bar", "baz"));
      //  assertTrue(client.deleteFile("baz"));
        
        // passive
        assertTrue(client.setFileType(FTP.BINARY_FILE_TYPE));
        client.enterLocalPassiveMode();
        assertEquals("/foo",client.printWorkingDirectory());
        assertTrue(client.changeWorkingDirectory("/foo"));
        assertEquals("/foo",client.printWorkingDirectory());

        //TODO make a virtual filesystem that would work with directory
        //assertTrue(client.listFiles("/foo").length==1);
        
        assertTrue(client.storeFile("bar", new ByteArrayInputStream("content".getBytes())));
        assertTrue(client.rename("bar", "baz"));
       // client.deleteFile("baz");
        
        assertEquals(221,client.quit());
        try {
        	client.noop();
        	fail("Should throw exception");
        } catch (IOException e) {
        	//expected;
        } 
       
    }
}
