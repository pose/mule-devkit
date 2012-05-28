/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mule.devkit.maven.studio;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.sisu.equinox.launching.internal.P2ApplicationLauncher;
import org.jfrog.maven.annomojo.annotations.*;
import org.mule.devkit.maven.AbstractMuleMojo;
import org.mule.util.IOUtils;
import sun.security.tools.JarSigner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Build a Mule plugin archive.
 */
@MojoPhase("package")
@MojoGoal("studio-package")
@MojoRequiresDependencyResolution("runtime")
public class StudioPackageMojo extends AbstractMuleMojo {


    
    @MojoComponent
    private MavenProjectHelper projectHelper;
    @MojoParameter(expression = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    @MojoParameter(expression = "${keystore.path}", defaultValue = "${project.basedir}/keystore.ks")
    private String keystorePath;

    @MojoParameter(expression = "${alias}")
    private String alias;

    @MojoParameter(expression = "${licensePath}" , defaultValue = "${project.basedir}/studio_license.txt")
    private String licensePath;


    @MojoParameter(expression = "${storepass}")
    private String storepass;

    @MojoParameter(expression = "${keypass}")
    private String keypass;

    @MojoParameter(expression = "${category}")
    private String category;

    @MojoComponent
    private P2ApplicationLauncher launcher;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipStudioPluginPackage) {
            return;
        }

        String pluginVersion = buildVersion();
        String pluginName = project.getArtifactId();

        File studioFeature = featureBuilderFor(pluginVersion, pluginName).build();
        File studioPlugin = pluginBuilderFor(pluginVersion, pluginName).build();
         new StudioSiteXmlBuilder(pluginName,pluginVersion,finalName,
                 outputDirectory.getPath(),
                 classesDirectory, category).build();

        sign(studioFeature, studioPlugin);

        createContentAndArtifacts(pluginName, pluginVersion);



        projectHelper.attachArtifact(project, "zip", "us", buildZip());
    }

    private File buildZip()
    {
        ZipArchiver archiver = new ZipArchiver();

        File updateSiteDir = new File(outputDirectory + File.separator + "update-site" + File.separator);

        for ( File file : updateSiteDir.listFiles() )
        {
            if (file.isDirectory())
            {
                archiver.addDirectory(file, file.getName() +  File.separator);
            }
            else
            {
                archiver.addFile(file, file.getName());
            }
        }

        File destFile = new File(outputDirectory +  File.separator + "UpdateSite.zip");
        archiver.setDestFile(destFile);

        try {
            destFile.delete();
            archiver.createArchive();
        } catch (IOException e) {

        }


        return destFile;
    }

    private String buildVersion() throws MojoExecutionException {

        String pluginVersion = getPluginVersionFrom();

        return pluginVersion + "."+buildQualifier();
    }

    private String getPluginVersionFrom() throws MojoExecutionException {
        DefaultArtifactVersion av = new DefaultArtifactVersion(project.getVersion());

        int majorVersion = av.getMajorVersion();
        int minorVersion = av.getMinorVersion();
        int incrementalVersion = av.getIncrementalVersion();
        if ( majorVersion == 0 && minorVersion == 0 && incrementalVersion ==0 )
        {
            throw new MojoExecutionException("Invalid maven project version, can't create studio plugin version, format must be " +
                    "[major-version].[minor-version].[incremental-version]-[qualifier], at least a major version must be specified.");
        }
        return majorVersion + "." + minorVersion + "." + incrementalVersion;
    }

    private void sign(File studioFeature, File studioPlugin) {
        if ( keystorePath != null && checkExistenceOf(keystorePath))
        {
            JarSigner jarsigner = new JarSigner();

            List<String> pluginOptions = buildOptions(studioPlugin.getPath());
            List<String> featureOptions = buildOptions(studioFeature.getPath());

            jarsigner.run(pluginOptions.toArray(new String[0]));
            jarsigner.run(featureOptions.toArray(new String[0]));
        }
    }



    private List<String> buildOptions(String path) {
        ArrayList<String> options = new ArrayList<String>();
        options.add("-keystore");
        options.add(keystorePath);

        if ( storepass != null )
        {
            options.add("-storepass");
            options.add(storepass);
        }

        if ( keypass != null )
        {
            options.add("-keypass");
            options.add(keypass);
        }

        options.add("-verbose");
        options.add(path);
        options.add(alias);



         return options;
    }


    private void createContentAndArtifacts(String pluginName, String pluginVersion) throws MojoExecutionException{
        try{
           String updateSitePath = outputDirectory + File.separator + "update-site" + File.separator;

           launcher.addArguments("-metadataRepository", "file:" + updateSitePath);
           launcher.addArguments("-source", updateSitePath);
           launcher.addArguments("-artifactRepository", "file:" + updateSitePath);
           launcher.addArguments("-publishArtifacts", "-append");
           launcher.addArguments("-site",  "file:" + updateSitePath + "site.xml");
           launcher.setApplicationName("org.eclipse.equinox.p2.publisher.EclipseGenerator");

           launcher.execute(20);

            String jarName = pluginName + "_" + pluginVersion + ".jar";
            new File(updateSitePath + File.separator + "features" + File.separator + jarName).delete();
            new File(updateSitePath + File.separator + "plugins" + File.separator + jarName).delete();

        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Could not create update site", e);
        }
      
    }


    private StudioPluginBuilder pluginBuilderFor(String pluginVersion, String pluginName) {
        return new StudioPluginBuilder(pluginName,
                pluginVersion,
                finalName,
                outputDirectory.getPath(),
                classesDirectory, project.getVersion());
    }

    private StudioFeatureBuilder featureBuilderFor(String pluginVersion, String pluginName) throws MojoExecutionException {

        try {
            String license = "";

            if ( licensePath != null)
            {
                File licenseFile = new File(licensePath);
                if ( licenseFile.exists() )
                {
                    license = IOUtils.toString(new FileInputStream(licenseFile));
                }
            }

            return new StudioFeatureBuilder(pluginName,
                    pluginVersion,
                    finalName,
                    outputDirectory.getPath(),
                    license,
                    classesDirectory);
            
        } catch (FileNotFoundException e) {
           throw new MojoExecutionException("Invalid license Path", e);
        }

        
    }

    private String buildQualifier() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

        return sdf.format(new Date());
    }

    private boolean checkExistenceOf(String path) {
        return new File(path).exists();
    }

}
