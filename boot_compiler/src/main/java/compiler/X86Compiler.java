package compiler;

import javassist.NotFoundException;

import java.io.IOException;

public class X86Compiler {
    DataSection dataSection;
    CodeSection codeSection;

    public static void main(String[] args) throws NotFoundException, IOException {
        String in = args[0];
        String out = args[0];
        JarBundle bundle = new JarBundle(in);

        for (String cls : bundle.getClassNames()) {
            System.out.println(cls + " " +
                    bundle.getPool().getCtClass(cls).getMethods().length);
        }
    }
}
