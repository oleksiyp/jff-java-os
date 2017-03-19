package compiler.x86;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class RegisterDirective {
    public static final Pattern REG_RANGE = Pattern.compile("(.+)(\\d+)-(\\d+)(.*)");
    private final String name;
    private final String flags;
    private final int value;
    private final String type;

    public RegisterDirective(String line) {
        String[] fields = line.split("[\t ]+");
        name = fields[0];
        type = Arrays.stream(fields[1].split("_|:"))
                .map(InstructionDirective::capitalLetter)
                .collect(Collectors.joining());
        flags = fields[2];
        value = Integer.parseInt(fields[3]);
    }

    RegisterDirective(String name, String type, String flags, int value) {
        this.name = name;
        this.type = type;
        this.flags = flags;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() { return type; }

    public int getValue() {
        return value;
    }

    public List<RegisterDirective> recombineRegisters() {
        Matcher matcher = REG_RANGE.matcher(name);
        if (matcher.matches()) {
            String name = matcher.group(1);
            String from = matcher.group(2);
            String to = matcher.group(3);
            String suffix = matcher.group(4);

            List<RegisterDirective> list = new ArrayList<>();
            for (int i = Integer.parseInt(from); i <= Integer.parseInt(to); i++) {
                list.add(this
                        .withName(name + i + suffix)
                        .withValue(i));
            }
            return list;
        } else {
            return singletonList(this);
        }
    }

    private RegisterDirective withValue(int value) {
        return new RegisterDirective(name, type, flags, value);
    }

    private RegisterDirective withName(String name) {
        return new RegisterDirective(name, type, flags, value);
    }

    public int getNBytes() {
        if (flags.contains("reg8")) {
            return 1;
        } else if (flags.contains("reg16")) {
            return 2;
        } else if (flags.contains("reg32")) {
            return 4;
        } else if (flags.contains("reg64")) {
            return 8;
        } else if (flags.matches("([scdt]|fpu)reg")) {
            return 2;
        }
        throw new RuntimeException("no size, bad flags: " + this.flags);
    }

    public String getParentReg() {
        if (flags.contains("reg8")) {
            return "8";
        } else if (flags.contains("reg16")) {
            return "16";
        } else if (flags.contains("reg32")) {
            return "32";
        } else if (flags.contains("reg64")) {
            return "64";
        } else if (flags.matches("([scdt]|fpu)reg")) {
            return "";
        }
        throw new RuntimeException("no parent type, bad flags: " + this.flags);
    }
}
