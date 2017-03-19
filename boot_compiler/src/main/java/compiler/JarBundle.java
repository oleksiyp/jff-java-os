package compiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarBundle {
    private final ClassPool pool;
    private final List<String> classNames;

    public JarBundle(String jarFile) throws NotFoundException, IOException {
        pool = ClassPool.getDefault();
        pool.appendClassPath(jarFile);

        classNames = new ArrayList<>();

        try (JarInputStream in = new JarInputStream(
                new FileInputStream(jarFile), true)) {

            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                String fileName = entry.getName();

                if (fileName.endsWith(".class")) {
                    String className = fileName
                            .replace('/', '.')
                            .substring(0, fileName.length() - 6);

                    classNames.add(className);
                }
            }
        }
    }

    public ClassPool getPool() {
        return pool;
    }

    public List<String> getClassNames() {
        return classNames;
    }
}
