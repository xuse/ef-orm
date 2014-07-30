/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
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
package jef.tools.desktop;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * 剪贴板操作辅助类
 */
public class ClipBoard{  
    
    /*获取剪贴板中的字符串,如果不存在，则返回空*/  
    public static String getStringClipboard() throws UnsupportedFlavorException, IOException {  
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {  
            String text = (String)t.getTransferData(DataFlavor.stringFlavor);  
            return text;  
        }  
        return null;  
    }  
  
    /*把字符串写到剪贴板*/  
    public static void setStringClipboard(String str) {  
        StringSelection ss = new StringSelection(str);  
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);  
    }  
  
    /*获取剪贴板中的图片*/  
    public static Image getImageClipboard() throws UnsupportedFlavorException, IOException {  
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);  
        if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {  
            Image image = (Image)t.getTransferData(DataFlavor.imageFlavor);  
            return image;  
        }  
        return null;  
    }
    
    
  
    /*把图片写到剪贴板中*/  
    public static void setImageClipboard(Image image) {  
        ImageSelection imgSel = new ImageSelection(image);  
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);  
    }  
  
    public static class ImageSelection implements Transferable {  
        private Image image;  
  
        public ImageSelection(Image image) {  
            this.image = image;  
        }  
  
        // Returns supported flavors  
        public DataFlavor[] getTransferDataFlavors() {  
            return new DataFlavor[]{DataFlavor.imageFlavor};  
        }  
  
        // Returns true if flavor is supported  
        public boolean isDataFlavorSupported(DataFlavor flavor) {  
            return DataFlavor.imageFlavor.equals(flavor);  
        }  
  
        // Returns image  
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {  
            if (!DataFlavor.imageFlavor.equals(flavor)) {  
                throw new UnsupportedFlavorException(flavor);  
            }  
            return image;  
        }  
    }  
}  
