package compiler.x86;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class InstructionDirective {
    private final String name;
    private final String[] args;
    private final String directives;
    private final String[] attributes;

    public InstructionDirective(String line) {
        String[] parts = line.trim().split("\\[|]");
        if (parts.length == 1) {
            parts = parts[0].split("\t+");
            name = parts[0].trim();
            args = new String[]{parts[1].trim()};
            directives = parts[2].trim();
            attributes = new String[]{parts[3].trim()};
        } else {
            int namePartIdx = parts[0].indexOf('\t');
            if (namePartIdx == -1) {
                namePartIdx = parts[0].indexOf(' ');
            }
            name = parts[0].substring(0, namePartIdx).trim()
                    .replace("int", "interrupt");
            String argsPart = parts[0].substring(namePartIdx).trim();

            directives = parts[1].trim();
            attributes = parts[2].trim().split("\\s*,\\s*");
            args = Arrays.stream(
                    argsPart.split("\\s*,\\s*"))
                    .filter(str -> !str.equals("void"))
                    .map(str -> str.replace("*", ""))
                    .map(str -> str.replaceAll("(.+)\\|(far|near|short|to)", "$2_$1"))
                    .map(str ->
                            Arrays.stream(str.split("_|:"))
                                    .map(InstructionDirective::capitalLetter)
                                    .collect(Collectors.joining())
                    )
                    .collect(Collectors.toList())
                    .toArray(new String[0]);

        }
    }

    public InstructionDirective(String name, String[] args, String directives, String[] attributes) {
        this.name = name;
        this.args = args;
        this.directives = directives;
        this.attributes = attributes;
    }

    public List<InstructionDirective> recombinateArgs(List<String> combineArgs, int n) {
        if (n == args.length) {
            return Collections.singletonList(new InstructionDirective(name,
                    combineArgs.toArray(new String[combineArgs.size()]),
                    directives,
                    attributes));
        }

        List<InstructionDirective> result = new ArrayList<>();
        for (String arg : args[n].split("\\|")) {
            combineArgs.add(arg);
            result.addAll(recombinateArgs(combineArgs, n + 1));
            combineArgs.remove(combineArgs.size() - 1);
        }
        return result;
    }

    public String signature() {
        String nameConverted = Arrays.stream(this.name.toLowerCase().split("_"))
                .map(s -> s.toLowerCase())
                .collect(Collectors.joining());

        Map<String, Integer> idx = new HashMap<>();
        Map<String, Integer> nameRepeat = Arrays.stream(this.args)
                .collect(Collectors.toMap(Function.identity(),
                        (s) -> 1,
                        (v1, v2) -> v1 + v2));

        String argsConverted = Arrays.stream(this.args)
                .map(str -> str + " " + str.toLowerCase() +
                        (nameRepeat.get(str) >= 2 ? argIndex(idx, str) : ""))
                .collect(Collectors.joining(", "));


        return nameConverted + "(" + argsConverted + ")";
    }

    private String argIndex(Map<String, Integer> argIndexes, String arg) {
        Integer id = argIndexes.get(arg);
        if (id == null) {
            id = 0;
        } else {
            id++;
        }
        argIndexes.put(arg, id);
        return "" + (char) ('a' + id);
    }

    public static String capitalLetter(String s) {
        if (s.length() <= 1) return s.toUpperCase();
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public String getName() {
        return name;
    }

    public String[] getArgs() {
        return args;
    }

    public String getDirectives() {
        return directives;
    }

    public String[] getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "InstructionDirective{" +
                "name='" + name + '\'' +
                ", args=" + Arrays.toString(args) +
                ", directives='" + directives + '\'' +
                ", attributes=" + Arrays.toString(attributes) +
                '}';
    }

}
