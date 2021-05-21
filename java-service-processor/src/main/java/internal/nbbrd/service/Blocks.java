package internal.nbbrd.service;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

@lombok.experimental.UtilityClass
public class Blocks {

    public static CodeBlock casting(TypeName type, CodeBlock block) {
        return CodeBlock.of("(($T)$L)", type, block);
    }
}
