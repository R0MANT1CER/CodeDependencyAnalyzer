package com.analyzer.monitor;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Consumer;

public class FileChangeMonitor {
    private static final Logger logger = LoggerFactory.getLogger(FileChangeMonitor.class);
    private FileAlterationMonitor monitor;

    /**
     * 启动文件监听
     */
    public void startMonitoring(String projectPath, Consumer<File> onFileChanged) throws Exception {
        FileAlterationObserver observer = new FileAlterationObserver(projectPath, file -> file.getName().endsWith(".java"));
        
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                logger.info("检测到文件修改: {}", file.getName());
                onFileChanged.accept(file);
            }

            @Override
            public void onFileCreate(File file) {
                logger.info("检测到新文件: {}", file.getName());
                onFileChanged.accept(file);
            }

            @Override
            public void onFileDelete(File file) {
                logger.info("检测到文件删除: {}", file.getName());
                onFileChanged.accept(file);
            }
        };
        
        observer.addListener(listener);
        
        // 每2秒检查一次
        monitor = new FileAlterationMonitor(2000);
        monitor.addObserver(observer);
        monitor.start();
        
        logger.info("✓ 文件监听已启动");
    }

    /**
     * 停止监听
     */
    public void stopMonitoring() throws Exception {
        if (monitor != null) {
            monitor.stop();
            logger.info("✓ 文件监听已停止");
        }
    }
}
