package com.github.anno4j.rdfs_parser.naming;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for {@link ClassNameBuilder}.
 */
public class ClassNameBuilderTest {
    @Test
    public void className() throws Exception {
        ClassName cn = ClassNameBuilder.builder("http://example.com/ont#Person")
                .className();
        assertEquals("com.example", cn.packageName());
        assertEquals("Person", cn.simpleName());

        cn = ClassNameBuilder.builder("http://example.com/person")
                .className();
        assertEquals("com.example", cn.packageName());
        assertEquals("Person", cn.simpleName());
    }

    @Test
    public void interfaceSpec() throws Exception {
        TypeSpec typeSpec = ClassNameBuilder.builder("http://example.com/ont#Person")
                .interfaceSpec();
        assertEquals("Person", typeSpec.name);

        typeSpec = ClassNameBuilder.builder("http://example.com/person")
                .interfaceSpec();
        assertEquals("Person", typeSpec.name);
    }

}