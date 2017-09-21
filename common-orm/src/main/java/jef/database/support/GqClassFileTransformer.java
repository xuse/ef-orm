package jef.database.support;

import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.ProtectionDomain;

import jef.codegen.EnhanceTaskASM;
import jef.tools.IOUtils;
import jef.tools.resource.ClasspathLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GqClassFileTransformer implements java.lang.instrument.ClassFileTransformer {
    private Logger log = LoggerFactory.getLogger(GqClassFileTransformer.class);

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        if (name.startsWith("jef/database") || name.startsWith("org/apache") || name.startsWith("javax/"))
            return null;
        if (name.indexOf('$') > -1) {
            return null;
        }
        URL u1 = loader.getResource(name.replace('.', '/') + ".class");
        if (u1 == null)
            return null;

        URL u2 = loader.getResource(name.replace('.', '/') + "$Field.class");
        EnhanceTaskASM task = new EnhanceTaskASM(new ClasspathLoader(false, loader));
        byte[] enhanced;
        try {
            enhanced = task.doEnhance(IOUtils.toByteArray(u1), u2 == null ? null : IOUtils.toByteArray(u2));
        } catch (Exception e) {
            log.error("类动态增强错误", e);
            return null;
        }
        if (enhanced == null || enhanced.length == 0) {
            return null;
        }
        log.info("Runtime Enhance Class For Easyframe ORM: {}", name);
        return enhanced;
    }

}
