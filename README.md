## graftt

- compares jars or class folders for API differences

### Stuff under the hood


- the gist: jar/directory -> parse classes -> NodeFactory(ClassLoader) -> model

- ASM for initial parsing class parsing
  - (planned) avoid `ClassNotFoundException` by stubbing + clearing method instructions
- `Class.forName` using a URLClassLoader per jar/directory    
- reflection: resolves inheritance
  - currently only java
  
  