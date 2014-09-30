/*
 ** 2014 September 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.unity.gui.control;

import info.ata4.io.buffer.ByteBufferUtils;
import info.ata4.unity.assetbundle.AssetBundleUtils;
import info.ata4.unity.assetbundle.BufferedEntry;
import info.ata4.unity.gui.util.DialogUtils;
import info.ata4.unity.gui.util.progress.ProgressTask;
import info.ata4.unity.rtti.FieldNode;
import info.ata4.unity.rtti.ObjectData;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class AssetFileTreePopup extends JPopupMenu {

    public AssetFileTreePopup(int selRow, TreePath selPath) {
        Object lastComponent = selPath.getLastPathComponent();
        if (!(lastComponent instanceof DefaultMutableTreeNode)) {
            return;
        }
        
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) lastComponent;
        
        Object userObject = treeNode.getUserObject();
        if (userObject instanceof ObjectData) {
            final ObjectData objectData = (ObjectData) userObject;
            JMenuItem item = new JMenuItem("Extract raw object data");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    extractByteBuffer(objectData.getBuffer(), objectData.getName());
                }
            });
            add(item);
        } else if (userObject instanceof Path) {
            final Path file = (Path) userObject;
            if (AssetBundleUtils.isAssetBundle(file)) {
                JMenuItem item = new JMenuItem("Extract asset bundle");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        extractAssetBundle(file);
                    }
                });
                add(item);
            }
        } else if (userObject instanceof BufferedEntry) {
            final BufferedEntry entry = (BufferedEntry) userObject;
            JMenuItem item = new JMenuItem("Extract file");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    extractAssetBundleEntry(entry);
                }
            });
            add(item);
        } else if (userObject instanceof FieldNode) {
            final FieldNode fieldNode = (FieldNode) userObject;
            Object fieldValue = fieldNode.getValue();
            if (fieldValue instanceof ByteBuffer) {
                final ByteBuffer bb = (ByteBuffer) fieldValue;
                JMenuItem item = new JMenuItem("Extract array data");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        extractByteBuffer(bb, fieldNode.getType().getFieldName());
                    }
                });
                add(item);
            }
        }
    }
    
    private void extractByteBuffer(ByteBuffer bb, String name) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(String.format("%s.bin", name)));
        
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        Path file = fileChooser.getSelectedFile().toPath();
        bb.rewind();
        
        try {
            ByteBufferUtils.save(file, bb);
        } catch (IOException ex) {
            DialogUtils.exception(ex, "Error saving file " + file.getFileName());
        }
    }
    
    private void extractAssetBundle(Path file) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(file.getParent().toFile());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        Path dir = fileChooser.getSelectedFile().toPath();
        
        new AssetBundleExtractTask(this, file, dir).execute();
    }
    
    private void extractAssetBundleEntry(BufferedEntry entry) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(entry.getInfo().getName()));
        
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        Path file = fileChooser.getSelectedFile().toPath();
        
        try {
            Files.copy(entry.getReader().getSocket().getInputStream(), file);
        } catch (IOException ex) {
            DialogUtils.exception(ex, "Error saving file " + file.getFileName());
        }
    }
    
    private class AssetBundleExtractTask extends ProgressTask<Void, Void> {

        private final Path file;
        private final Path dir;

        private AssetBundleExtractTask(Component parent, Path file, Path dir) {
            super(parent, "Extracting asset bundle", "");
            this.file = file;
            this.dir = dir;
        }

        @Override
        protected Void doInBackground() throws Exception {
            AssetBundleUtils.extract(file, dir, progress);
            return null;
        }
    }
}
