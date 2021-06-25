package gin.edit.line;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import  com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import gin.SourceFile;
import gin.SourceFileLine;
import gin.edit.Edit;

import java.util.*;

public class CacheEdit extends LineEdit {


    public String sourceFile;
    private List<Integer> toCache;
    private String parentMethod;
    private String method;
    private ResolvedType type;

    public CacheEdit(SourceFile sf, Random rng) {
        SourceFileLine sourceFile = (SourceFileLine) sf;
        this.sourceFile = sourceFile.getFilename();
        Map<MethodDeclaration, Map<String, List<MethodCallExpr>>> cachePoint = sourceFile.getCachePoint();
        MethodDeclaration dec = (MethodDeclaration) cachePoint.keySet().toArray()[rng.nextInt(cachePoint.keySet().size())];
        parentMethod = dec.getDeclarationAsString(true, true, true);
        toCache = new ArrayList<>();
        Map<String, List<MethodCallExpr>> methodMap = cachePoint.get(dec);
        String methodName = (String) methodMap.keySet().toArray()[rng.nextInt(methodMap.keySet().size())];
        method = methodMap.get(methodName).get(0).getTokenRange().get().toString();
        type = methodMap.get(methodName).get(0).resolve().getReturnType();
        while(toCache.size() < 2) {
            for (int call=0; call < methodMap.get(methodName).size();call++) {
                float coinFlip = rng.nextFloat();
                if (coinFlip < 0.5) {
                    if (!toCache.contains(call)) {
                        toCache.add(call);
                    }
                }
            }
        }
    }

    public CacheEdit(String sourceFile, String methodDec, String methodCall, ArrayList<Integer> toCache){

    }

    @Override
    public SourceFile apply(SourceFile sourceFile) {
        String id =  "" + System.currentTimeMillis();
        int point = 0;
        ArrayList<Integer> lineNos = new ArrayList<>();
        TypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        ParserConfiguration configuration = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
        JavaParser parser = new JavaParser(configuration);
        CompilationUnit compilationUnit = parser.parse(sourceFile.getSource()).getResult().get();
        for (Node child : compilationUnit.getChildNodes()){
            for (MethodDeclaration methodDec : child.findAll(MethodDeclaration.class)){
                if(methodDec.getDeclarationAsString(true,true,true).equals(parentMethod)){
                    for(MethodCallExpr methodCall : methodDec.findAll(MethodCallExpr.class)){
                        if(methodCall.getTokenRange().get().toString().equals(method)){
                            if (toCache.contains(point)){
                                lineNos.add(methodCall.getRange().get().begin.line);
                            }
                            point++;
                        }
                    }
                }

            }
        }
        String typeName;
        sourceFile = (SourceFileLine) sourceFile;

        if (type.isPrimitive()){
            typeName = type.asPrimitive().name().toLowerCase(Locale.ROOT);
        }
        else{
            typeName = type.asReferenceType().getQualifiedName();
        }
        String varName = "ginCachedVar" +  System.currentTimeMillis();
        String cacheString = typeName + " " + varName +  " = " + method + ";";

        sourceFile = ((SourceFileLine) sourceFile).insertLine(lineNos.get(0)-1, cacheString);
        for (Integer lineNo : lineNos){
            String line = ((SourceFileLine) sourceFile).getLine(lineNo);
            sourceFile = ((SourceFileLine) sourceFile).removeLine(lineNo);
            line = line.replace(method, varName);
            sourceFile = ((SourceFileLine) sourceFile).insertLine(lineNo,line);
        }
        return  sourceFile;
    }

    public String toString(){
        String out =this.getClass().getCanonicalName() + " " + sourceFile + " " + parentMethod + " " + method;
        for (int call : toCache){
            out += " "  + call;
        }
        return out;
    }

    public static CacheEdit fromString(String str){
        String[] tokens = str.split( " ");
        String fileName = tokens[0];
        String parentMethod = tokens[1];
        String method = tokens[2];
        ArrayList<Integer> toCache = new ArrayList<>();
        for (String point: Arrays.copyOfRange(tokens, 3, tokens.length)){
            toCache.add(Integer.valueOf(point));
        }
        return new CacheEdit(fileName, parentMethod, method, toCache);

    }
}
