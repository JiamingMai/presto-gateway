package com.kapok.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
 
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
 
public class HttpServletRequestReader
{
 
    public static String readAsChars(HttpServletRequest request)
    {
 
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder("");
        try
        {
            br = request.getReader();
            String str;
            while ((str = br.readLine()) != null)
            {
                sb.append(str);
            }
            br.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != br)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
 
    public static void readAsChars2(HttpServletRequest request)
    {
        InputStream is = null;
        try
        {
            is = request.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] b = new byte[4096];
            for (int n; (n = is.read(b)) != -1;)
            {
                sb.append(new String(b, 0, n));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != is)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
 
    }
 
    public static byte[] readAsBytes(HttpServletRequest request)
    {
 
        int len = request.getContentLength();
        byte[] buffer = new byte[len];
        ServletInputStream in = null;
 
        try
        {
            in = request.getInputStream();
            in.read(buffer, 0, len);
            in.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != in)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return buffer;
    }
 
}