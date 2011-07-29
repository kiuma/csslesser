/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.wingstech.csslesser;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 *
 * @author kiuma
 */
@Test
public class CompressMojoTest
{
    public void testStripComments() throws Exception
    {
        Assert.assertEquals("/*comment/*xx*/foo/*comment/*xx*/bar".replaceAll("(/\\*([^*]|(\\*+[^*/]))*\\*+/)", ""),
                            "foobar");     
    }
    
    public void testStripCommentsLineBreaks() throws Exception
    {
        Assert.assertEquals("/*comment/*xx*/foo\n/*comment/*xx*/bar".replaceAll("(/\\*([^*]|(\\*+[^*/]))*\\*+/)", ""),
                            "foo\nbar");     
    }
    
    public void testCssInclude() throws Exception
    {        
        Pattern p = Pattern.compile(
                "(" + Pattern.quote("@import url(") + "[\\'\\\"]?)(.*?)([\\'\\\"]?"+ Pattern.quote(");") + ")");
        
        Matcher m = p.matcher("@import url(claro/claro.css);@import url(dashboard.css);@import url(dojodashboard.css);");   
        StringBuffer sb = new StringBuffer();
        while (m.find() == true)                
        {            
            m.appendReplacement(sb, m.group(2));
        }
        Assert.assertEquals(sb.toString(),"claro/claro.cssdashboard.cssdojodashboard.css");
        
    }

    public void testCssImagePath() throws Exception
    {
        Pattern p = Pattern.compile(
                "(" + Pattern.quote("url(") + "[\\'\\\"]?)(.*?)([\\'\\\"]?"+
                        Pattern.quote(")") +
                        ")");
        String str = "background-image: url(claro/claro.png);background-image: url(dashboard.css);background-image: url(/dojodashboard.css); color: green;";
        String finalStr = "background-image: url('foo/claro/claro.png');background-image: url('foo/dashboard.css');background-image: url('/dojodashboard.css'); color: green;";
        Matcher m = p.matcher(str);
        StringBuffer sb = new StringBuffer();
        String prefix = "foo";

        while (m.find() == true)
        {
            String url = m.group(2);
            if (!url.startsWith("/"))
            {
                url = "url('" + prefix + "/" + url + "'";
            }
            else
            {
                url = "url('" + url + "'";
            }
            m.appendReplacement(sb, url + m.group(3));

            //m.appendReplacement(sb, m.group(2));
        }
        m.appendTail(sb);

        Assert.assertEquals(sb.toString(),finalStr);

    }

    public void testRelPathPath() throws Exception
    {
//        File f = new File("home/kiuma/foo.css");
//        Assert.assertEquals("home/kiuma", f.getParent());
//        Assert.assertEquals("home/kiuma", "home\\kiuma".replaceAll("\\\\", "/"));
    }

    public void testMinimizePath() throws Exception
    {
        File f = new File("./kiuma/foo.css");
        File f2 = new File(".");

        String path = f.getCanonicalPath().substring(f2.getCanonicalPath().length() + 1).replaceAll("\\\\", "/");
        Assert.assertEquals("kiuma/foo.css", path);
    }
}
