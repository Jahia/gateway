/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 */
package org.jahia.modules.gateway.mail;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Data object for holding parsed e-mail data.
 * 
 * @author Cédric Mailleux
 */
public class MailContent {

    public static class FileItem {

        private String contentType;

        private File file;

        private String name;

        public FileItem(String name, File file) {
            this(name, file, null);
        }

        public FileItem(String name, File file, String contentType) {
            super();
            this.name = name;
            this.file = file;
            this.contentType = contentType;
        }

        public String getContentType() {
            return contentType;
        }

        public File getFile() {
            return file;
        }

        public String getName() {
            return name;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "FileItem [name=" + name + ", contentType=" + contentType + ", file=" + file
                    + "]";
        }

    }

    private String body = null;
    
    private List<FileItem> files = new LinkedList<FileItem>();
    
    private boolean isHtml;

    public String getBody() {
        return body;
    }

    public List<FileItem> getFiles() {
        return files;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setBodyHtml(String body) {
        this.body = body;
        isHtml = true;
    }

    public void setFiles(List<FileItem> files) {
        this.files = files;
    }

    public void setHtml(boolean isHtml) {
        this.isHtml = isHtml;
    }

    @Override
    public String toString() {
        return "MailContent [body=" + body + ", files=" + files + "]";
    }

}
