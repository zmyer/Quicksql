package com.qihoo.qsql.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collect generated code of each part of query to generate a completed Java Class code. <p> Responsible for collecting
 * every data source's execution code which is generated by each of {@link QueryGenerator}, adding the header code of a
 * Class and the interpreter execution code from {@link ClassBodyWrapper}, and finally generated a compilable Java Class
 * code. </p>
 */
public class ClassBodyComposer {

    private BlockLink composer;

    /**
     * Collect all the generated code as long as Composer is initialized.
     */
    public ClassBodyComposer() {
        composer =
            new ImportsLink(
                new ClassesLink(
                    new InnerClassesLink(
                        new MethodsLink(
                            new SentencesLink()))));
    }

    /**
     * Return code of the complete Java Class.
     *
     * @return class body
     */
    public String getCompleteClass() {
        StringBuilder builder = new StringBuilder();
        composer.compose(builder);
        return builder.toString();
    }

    /**
     * Compose all the codes based on category.
     *
     * @param category CodeCategory, e.g., IMPORT
     * @param codes Generated codes
     */
    public void handleComposition(CodeCategory category, String... codes) {
        composer.decorateTrait(category.category, codes);
    }

    public enum CodeCategory {
        IMPORT(ImportsLink.class),
        CLASS(ClassesLink.class),
        INNER_CLASS(InnerClassesLink.class),
        METHOD(MethodsLink.class),
        SENTENCE(SentencesLink.class);

        Class category;

        CodeCategory(Class category) {
            this.category = category;
        }
    }

    /**
     * Provide different types of class to help {@link ClassBodyComposer} to compose code in order.
     */
    abstract class BlockLink {

        BlockLink successor;

        BlockLink(BlockLink link) {
            this.successor = link;
        }

        protected abstract void decorateTrait(Class clazz, String... code);

        boolean isMyResponsibility(Class clazz) {
            return this.getClass().equals(clazz);
        }

        public abstract void compose(StringBuilder builder);
    }

    class ImportsLink extends BlockLink {

        private Set<String> imports = new HashSet<>();

        ImportsLink(BlockLink link) {
            super(link);
        }

        @Override
        protected void decorateTrait(Class clazz, String... code) {
            if (isMyResponsibility(clazz)) {
                imports.addAll(Arrays.asList(code));
            } else {
                successor.decorateTrait(clazz, code);
            }
        }

        @Override
        public void compose(StringBuilder builder) {
            for (String im : imports) {
                builder.append(im).append(";\n");
            }
            successor.compose(builder);
        }
    }

    class ClassesLink extends BlockLink {

        private String className = "DefaultRequirement_0";

        ClassesLink(BlockLink link) {
            super(link);
        }

        //TODO change to generate construction dynamically
        @Override
        protected void decorateTrait(Class clazz, String... code) {
            if (isMyResponsibility(clazz)) {
                if (code.length < 1) {
                    throw new RuntimeException("Need a class name");
                }
                className = code[0];
            } else {
                successor.decorateTrait(clazz, code);
            }
        }

        @Override
        public void compose(StringBuilder builder) {
            builder.append("\n");
            builder.append("public class ").append(className)
                .append(" extends SparkRequirement { \n");

            builder.append("\t\tpublic ").append(className).append("(SparkSession spark){\n")
                .append("\t\t\tsuper(spark);\n"
                    + "\t\t}");

            successor.compose(builder);

            builder.append("}\n");
        }
    }

    class InnerClassesLink extends BlockLink {

        private List<String> innerClasses = new ArrayList<>();

        InnerClassesLink(BlockLink link) {
            super(link);
        }

        @Override
        protected void decorateTrait(Class clazz, String... code) {
            if (isMyResponsibility(clazz)) {
                innerClasses.addAll(Arrays.asList(code));
            } else {
                successor.decorateTrait(clazz, code);
            }
        }

        @Override
        public void compose(StringBuilder builder) {
            builder.append("\n");

            for (String classes : innerClasses) {
                builder.append(classes).append("\n");
            }
            successor.compose(builder);
        }
    }

    class MethodsLink extends BlockLink {

        private List<String> methods = new ArrayList<>();

        MethodsLink(BlockLink link) {
            super(link);
        }

        @Override
        protected void decorateTrait(Class clazz, String... code) {
            if (isMyResponsibility(clazz)) {
                methods.addAll(Arrays.asList(code));
            } else {
                successor.decorateTrait(clazz, code);
            }
        }

        @Override
        public void compose(StringBuilder builder) {
            for (String method : methods) {
                builder.append(method).append("\n");
            }
            successor.compose(builder);
        }
    }

    class SentencesLink extends BlockLink {

        private List<String> sentences = new ArrayList<>();

        SentencesLink() {
            super(new EmptyLink());
        }

        @Override
        protected void decorateTrait(Class clazz, String... code) {
            if (isMyResponsibility(clazz)) {
                sentences.addAll(Arrays.asList(code));
            }
        }

        @Override
        public void compose(StringBuilder builder) {
            builder.append("\n");
            builder.append("\t\tpublic void execute(){\n");
            builder.append("\t\t\tDataset<Row> tmp;\n");

            for (String sentence : sentences) {
                builder.append("\t\t\t").append(sentence).append("\n");
            }

            builder.append("\t\t}\n");
        }
    }

    class EmptyLink extends BlockLink {

        EmptyLink() {
            super(null);
        }

        @Override
        protected void decorateTrait(Class clazz, String... code) {
            throw new RuntimeException("Empty Link");
        }

        @Override
        public void compose(StringBuilder builder) {
            throw new RuntimeException("Empty Link");
        }
    }
}
