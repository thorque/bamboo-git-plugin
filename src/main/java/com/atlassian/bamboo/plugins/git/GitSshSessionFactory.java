package com.atlassian.bamboo.plugins.git;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.jetbrains.annotations.Nullable;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class GitSshSessionFactory extends JschConfigSessionFactory
{
    final private String key;
    final private String passphrase;

    GitSshSessionFactory(@Nullable final String key, @Nullable final String passphrase)
    {
        this.key = key;
        this.passphrase = passphrase;
    }

    @Override
    protected void configure(OpenSshConfig.Host hc, Session session)
    {
        session.setConfig("StrictHostKeyChecking", "no");
    }

    protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
        JSch jsch = super.getJSch(hc, fs);
        jsch.removeAllIdentity();
        if (StringUtils.isNotEmpty(key))
        {
            jsch.addIdentity("identityName", key.getBytes(), null, passphrase.getBytes());
        }
        return jsch;
    }
}
