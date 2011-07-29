package it.wingstech.csslesser;

/*
 * Copyright 2011 Andrea Chiumenti.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Goal which touches a timestamp file.
 *
 * @goal lessify
 * @phase process-resources
 * @requiresDependencyResolution runtime
 * 
 */
public class LessifyMojo
        extends AbstractMojo
{

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Default resource folder
     * @parameter expression="src/main/less"
     */
    private String srcFolderName;

    /**
     * Folder where generated sources will go
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     */
    private String outputFolderName;

    /**
     * The name of the generated war
     * @parameter
     */
    private Resource[] lessResources;


    /**
     * Performs css compression;
     *
     * @parameter default-value=true
     */
    private boolean cssCompress;

    /**
     * Performs less evaluation;
     *
     * @parameter default-value=true
     */
    private boolean lessify;

    /**
     * When css compression is enabled, keeps or removes css comments;
     *
     * @parameter default-value=true
     */
    private boolean cssStripComments;

    /**
     * When css compression is enabled, keeps or removes line breaks;
     *
     * @parameter default-value=true
     */
    private boolean cssSingleLine;

    /**
     * Performs css inline inclusion;
     *
     * @parameter default-value=true
     */
    private boolean cssInline;

    @Override
    public void execute()
            throws MojoExecutionException
    {
        srcFolderName = srcFolderName.replace('\\', File.separatorChar).replace('/',File.separatorChar);
        outputFolderName = outputFolderName.replace('\\', File.separatorChar).replace('/',File.separatorChar);
        if (lessResources == null || lessResources.length == 0)
        {
            lessResources = new Resource[1];
            lessResources[0] = new Resource();
            lessResources[0].setDirectory(srcFolderName);
        }
        for (Resource resource : lessResources)
        {
            String[] resources = getIncludedFiles(resource);
            String directory = resource.getDirectory();//.replace('\\', File.separatorChar).replace('/',File.separatorChar);
            getLog().info("Copying resources...");
            for (String path : resources)
            {
                try
                {
                    FileUtils.copyFile(
                            new File(project.getBasedir() + File.separator + directory + File.separator + path),
                            new File(outputFolderName + File.separator + path));
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
            if (lessify)
            {
                getLog().info("Performing less transformation...");
                LessEngine engine = new LessEngine();
                for (String path : resources)
                {
                    if (path.toUpperCase().endsWith(".LESS"))
                    {
                        try
                        {
                            File inputFile = new File(outputFolderName + File.separator + path);
                            File outputFile = new File(outputFolderName + File.separator + path.substring(0, path.length() - 5) + ".css");

                            getLog().info("LESS processing file: " + path + " ...");
                            engine.compile(inputFile, outputFile);
                        }
                        catch (Exception e)
                        {
                            throw new MojoExecutionException(e.getMessage(), e);
                        }
                    }
                }
            }
            if (cssCompress)
            {
                getLog().info("Performing css compression...");
                Collection<File> inputFiles = FileUtils.listFiles(new File(outputFolderName), new String[]{"css"}, true);
                for (File inputFile : inputFiles)
                {
                    getLog().info("Compressing file: " + inputFile.getPath() + " ...");
                    compress(inputFile);
                }
            }
            if (cssInline)
            {
                getLog().info("Performing css inlining...");
                Collection<File> inputFiles = FileUtils.listFiles(new File(outputFolderName), new String[]{"css"}, true);
                for (File inputFile : inputFiles)
                {
                    getLog().info("Inlining file: " + inputFile.getPath() + " ...");
                    inline(inputFile, ".");
                }
            }
        }
    }

    private String[] getIncludedFiles(Resource resource)
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(project.getBasedir() + File.separator +resource.getDirectory());
        List<String> excludes = resource.getExcludes();
        if (CollectionUtils.isNotEmpty(excludes))
        {
            scanner.setExcludes(excludes.toArray(new String[excludes.size()]));
        }
        List<String> includes = resource.getIncludes();
        if (CollectionUtils.isNotEmpty(includes))
        {
            scanner.setIncludes(includes.toArray(new String[includes.size()]));
        }
        scanner.scan();
        return scanner.getIncludedFiles();

    }

    private void compress(File f) throws MojoExecutionException
    {
        if (f.isFile())
        {
            try
            {
                String result = FileUtils.readFileToString(f).replaceAll("\\s+", " ").replaceAll("\\s*\\{\\s*", "{").replaceAll("\\s*\\}\\s*", "} "); //minimize spaces
                if (cssStripComments)
                {
                    result = result.replaceAll("(/\\*([^*]|(\\*+[^*/]))*\\*+/)", ""); //strip comments
                }
                if (cssSingleLine)
                {
                    result = result.replaceAll("[\r\n]", ""); //strip line breaks
                }
                FileUtils.writeStringToFile(f,result);
            }
            catch (Exception ex)
            {
                throw new MojoExecutionException(ex.getMessage() + " on file " + f.getAbsolutePath(), ex);
            }
        }
    }

    private String inline(File f, String prefix) throws MojoExecutionException
    {
        if (f.isFile())
        {
            getLog().info("Inlining file: " + f.getAbsolutePath() + " ...");
            try
            {
                String content = FileUtils.readFileToString(f);
                Pattern p = Pattern.compile(
                "(" + Pattern.quote("@import url(") + "[\\'\\\"]?)(.*?)([\\'\\\"]?"+ Pattern.quote(");") + ")");

                Matcher m = p.matcher(content);
                StringBuffer sb = new StringBuffer();

                while (m.find() == true)
                {
                    String url = m.group(2);
                    getLog().info("   importing file: " + url + " ...");
                    String cPrefix = ".";
                    int ix = url.lastIndexOf("/");
                    if (ix > 0)
                    {
                        cPrefix = url.substring(0, ix);
                    }

                    String inputReplacement = inline(new File(f.getParentFile().getAbsolutePath() + File.separator + m.group(2)),
                            cPrefix);
                    m.appendReplacement(sb, inputReplacement);
                }
                m.appendTail(sb);

                String result = sb.toString();

                FileUtils.writeStringToFile(f, result);


                StringBuffer sbIncludeImages = new StringBuffer();
                Pattern pImgReplacement = Pattern.compile(
                        "(" + Pattern.quote("url(") + "[\\'\\\"]?)(.*?)([\\'\\\"]?" +
                                Pattern.quote(")") +
                                ")");
                Matcher mImgReplacement = pImgReplacement.matcher(result);
                while (mImgReplacement.find() == true)
                {
                    String urlImage = mImgReplacement.group(2);

                    if (!urlImage.startsWith("/"))
                    {
                        urlImage = prefix + "/" + urlImage;//minimizePath(new File(currPrefix + "/" + urlImage));
                    }
                    mImgReplacement.appendReplacement(sbIncludeImages, "url('" + urlImage + "')");

                }
                mImgReplacement.appendTail(sbIncludeImages);


                return sbIncludeImages.toString();

            }
            catch (Exception ex)
            {
                throw new MojoExecutionException(ex.getMessage() + " on file " + f.getAbsolutePath(), ex);
            }
        }
        return "";
    }

    public String minimizePath(File f) throws Exception
    {
        File f2 = new File(".");
        String path = f.getCanonicalPath().substring(f2.getCanonicalPath().length() + 1).replaceAll("\\\\", "/");
        return path;
    }
//    public Resource[] getLessResources()
//    {
//        return lessResources;
//    }
//
//    public void setLessResources(Resource[] lessResources)
//    {
//        this.lessResources = lessResources;
//    }
}
