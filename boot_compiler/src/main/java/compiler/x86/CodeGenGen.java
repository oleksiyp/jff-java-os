package compiler.x86;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodeGenGen {
    public static final String NASM_INSNS_DAT = "resources/compiler/x86/insns.dat";
    public static final String REGS_DAT = "resources/compiler/x86/regs.dat";
    public static final String JAVA_DIR = "java";

    public static final String PACKAGE_NAME = "compiler.x86.generated";
    public static final String REGS_PACKAGE = PACKAGE_NAME + ".regs";
    public static final String X86_CODE_GENERATOR_CLASS = "X86CodeGenerator";
    public static final String X86_INSTRUCTION_SET_CLASS = "X86InstructionSet";

    private List<InstructionDirective> directives;
    private Map<String, List<RegisterDirective>> regs;

    public static void main(String[] args) throws IOException {
        CodeGenGen codeGenGen = new CodeGenGen();

        String srcMainDir = args[0];
        codeGenGen.parse(
                Paths.get(srcMainDir, NASM_INSNS_DAT),
                Paths.get(srcMainDir, REGS_DAT));

        Path genBasePath = Paths.get(srcMainDir, JAVA_DIR);
        codeGenGen.generateInstructionSetInterface(
                genBasePath,
                PACKAGE_NAME,
                X86_INSTRUCTION_SET_CLASS);

        codeGenGen.generateCodeGeneratorClass(
                genBasePath,
                PACKAGE_NAME,
                X86_CODE_GENERATOR_CLASS);

        codeGenGen.generateRegisterClasses(
                genBasePath,
                REGS_PACKAGE);
    }

    public void parse(Path insnsDat, Path regsDat) throws IOException {

        directives = Files.readAllLines(insnsDat)
                .stream()
                .map(CodeGenGen::removeComments)
                .filter(CodeGenGen::isNotAnEmpty)
                .map(InstructionDirective::new)
                .filter(CodeGenGen::isNotHLE)
                .flatMap(insn -> insn.recombinateArgs(new ArrayList<>(), 0).stream())
                .collect(Collectors.toList());

        regs = Files.readAllLines(regsDat)
                .stream()
                .map(CodeGenGen::removeComments)
                .filter(CodeGenGen::isNotAnEmpty)
                .map(RegisterDirective::new)
                .flatMap(registerDirective -> registerDirective.recombineRegisters().stream())
                .collect(Collectors.toMap(
                        RegisterDirective::getType,
                        Collections::singletonList,
                        (l1, l2) -> Stream.of(l1, l2)
                                .flatMap(List::stream)
                                .collect(Collectors.toList())
                        ));
    }


    public void generateCodeGeneratorClass(Path base, String packageName, String className) throws IOException {
        Path java = resolveJavaPathAndMkdir(base, packageName, className);

        try (PrintWriter out = new PrintWriter(java.toFile())) {
            out.println("package " + packageName + ";");
            out.println();
            out.println("import " + REGS_PACKAGE + ".*;");
            out.println();
            out.println("public class " + className + " implements X86InstructionSet<" + className + "> {");

            directives.stream()
                    .collect(Collectors.toMap(
                            InstructionDirective::signature,
                            Function.identity(),
                            (ins1, ins2) -> ins1,
                            TreeMap::new))
                    .forEach((signature, instructionDirective) ->
                    {
                        out.println(i(1) + "public " + className + " " + signature + " {");
                        out.println(i(2) + "// " + instructionDirective.getDirectives());
                        out.println(i(2) + "return this;");
                        out.println(i(1) + "}");
                    });

            out.println("}");
        }
    }


    private void generateRegisterClasses(Path base, String packageName) {
        directives.stream()
                .flatMap(dir -> Arrays.stream(dir.getArgs()))
                .collect(Collectors.<String, Set<String>>toCollection(TreeSet::new))
                .forEach(reg ->
                {
                    Path regJava = resolveJavaPathAndMkdir(base, packageName, reg);

                    try {
                        try (PrintWriter out = new PrintWriter(regJava.toFile())) {
                            out.println("package " + packageName + ";");
                            out.println();
                            out.print("public class " + reg);
                            generateRegisterClassCode(out, reg);
                        }
                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
                });

    }

    private void generateRegisterClassCode(PrintWriter out, String reg) {
        if (reg.equals("Reg")) {
            out.println(" {");
            out.println(i(1) + "private final int nBytes, val;");
            out.println();
            out.println(i(1) + "public Reg(int nBytes, int val) {");
            out.println(i(2) + "this.nBytes = nBytes;");
            out.println(i(2) + "this.val = val;");
            out.println(i(1) + "}");
            out.println(i(1) + "public int getNBytes() { return nBytes; }");
            out.println(i(1) + "public int getValue() { return val; }");
        } else if (reg.matches("Reg(\\d+)")) {
            out.println(" extends Reg {");
            int nBytes = Integer.parseInt(reg.substring(3)) / 8;
            out.println(i(1) + "public " + reg + "(int val) {");
            out.println(i(2) + "super(" + nBytes + ", val);");
            out.println(i(1) + "}");
        } else if (reg.matches("Reg([A-Za-z]+)")) {
            List<RegisterDirective> directive = regs.get(reg);
            if (directive == null || reg.equals("RegDreg")) {
                out.println(" {");
            } else if (directive.size() == 1) {
                RegisterDirective regDir = directive.get(0);
                int nBytes = regDir.getNBytes();

                out.println(" extends Reg" + regDir.getParentReg() + " {");
                out.println(i(1) + "public " + reg + "() {");
                out.println(i(2) + "super(" + regDir.getValue() + ");");
                out.println(i(1) + "}");
            } else {
                RegisterDirective regDir = directive.get(0);
                // they all are same sizes
                int nBytes = regDir.getNBytes();

                out.println(" extends Reg" + regDir.getParentReg() + " {");
                out.println(i(1) + "public " + reg + "(int val) {");
                out.println(i(2) + "super(val);");
                out.println(i(1) + "}");
            }
        } else {
            out.println(" {");
        }

        out.println("}");
    }

    public void generateInstructionSetInterface(Path base, String packageName, String className) throws IOException {
        Path java = resolveJavaPathAndMkdir(base, packageName, className);

        try (PrintWriter out = new PrintWriter(java.toFile())) {
            out.println("package " + packageName + ";");
            out.println();
            out.println("import " + REGS_PACKAGE + ".*;");
            out.println();
            out.println("public interface " + className + "<I extends " + className + "> {");

            directives.stream()
                    .map(InstructionDirective::signature)
                    .collect(Collectors.<String, Set<String>>toCollection(TreeSet::new))
                    .forEach((signature) -> out.println("    I " + signature + ";"));

            out.println("}");

        }
    }

    private Path resolveJavaPathAndMkdir(Path base, String packageName, String className) {
        String off = packageName.replaceAll("\\.", "/");
        Path dir = base.resolve(off);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
        }
        return dir.resolve(className + ".java");
    }


    private static boolean isNotHLE(InstructionDirective directive) {
        return !directive.getDirectives().contains("hle");
    }

    private static boolean isNotAnEmpty(String line) {
        return !line.trim().isEmpty();
    }

    private static String removeComments(String line) {
        return line.replaceAll(";.*", "")
                .replaceAll("#.*", "");
    }

    private static String i(int tabs) {
        if (tabs == 0) {
            return "";
        }
        return "    " + i(tabs - 1);
    }
}
