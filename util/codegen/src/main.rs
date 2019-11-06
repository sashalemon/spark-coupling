//! Scala boilerplate generator for the Spark end of libcourier
//! 
//! Depends on Clang for parsing C struct definitions
#[macro_use] extern crate askama;
extern crate colored;
extern crate clang;
#[macro_use] extern crate clap;
use std::fmt;
use std::io::Write;
use askama::Template;

#[derive(Template)]
#[template(path = "schema.scala")]

/// `Analysis` is purely for passing serializable data to templates
struct Analysis {
    structs: Vec<ParsedStruct>,
}

macro_rules! field_fmt {
    () => ("{:8}\t{:8}\t{:8}\t{:8}\t{:8}")
}

/// `ParsedField` is an intermediate data structure
/// It is mainly manipulated by the `ParsedStruct` implementation
#[derive(Debug)]
struct ParsedField {
    name: String,
    kind: String,
    size: usize,
    align: usize,
    offset: usize,
    java_type: String,
    javolution_type: String,
}

impl fmt::Display for ParsedField {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        use colored::*;
        write!(f, field_fmt!(),
               self.name.green(), 
               self.kind.purple(),
               self.size.to_string().red(),
               self.align.to_string().yellow(),
               self.align.to_string().blue()
              )
    }
}

/// `ParsedStruct` is an intermediate data structure
/// It obtains data from Clang's AST and implements serializer methods for it
#[derive(Debug)]
struct ParsedStruct {
    name: String,
    fields: Vec<ParsedField>,
}

impl fmt::Display for ParsedStruct {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        use colored::*;
        let mut field_strings = vec![];
        for field in self.fields.iter() {
            field_strings.push(format!("{}", field));
        }
        writeln!(f, "Struct: {}", self.name.green())
            .and_then(|()| writeln!(f, field_fmt!(), "Name", "Type", "Size", "Align", "Offset"))
            .and_then(|()| write!(f, "{}", field_strings.join("\n")))
    }
}

impl ParsedField {
    /// Stores data from a Clang entity into a `ParsedField`
    /// 
    /// * This should only be called by `ParsedStruct`
    /// * `record_type` and `field` are both obtained 
    /// within the `ParsedStruct` constructor
    fn new(record_type: &clang::Type, field: clang::Entity) -> ParsedField {
        use clang::TypeKind::{CharS, Int, Long, Float, Double};

        let field_name = field.get_display_name().expect("field missing name somehow");
        let kind = field.get_type().expect("field missing type somehow");
        let (java_type, javolution_type) = match kind.get_kind() {
            CharS => Ok(("Byte","Signed8")),
            Int  => Ok(("Int","Signed32")),
            Long => Ok(("Long","Signed64")),
            Float => Ok(("Float","Float32")),
            Double => Ok(("Double","Float64")),
            _ => Err("Unsupported")
        }.expect("Unsupported data type for output to Java");
        ParsedField {
            name: field_name.clone(),
            kind: kind.get_display_name(),
            size: kind.get_sizeof().expect("Size cannot be determined"),
            align: kind.get_alignof().expect("Alignment cannot be determined"),
            offset: record_type.get_offsetof(field_name).expect("Offset cannot be determined"),
            java_type: java_type.to_string(),
            javolution_type: javolution_type.to_string()
        }

    }
}

impl ParsedStruct {
    /// Mines data from a Clang declaration into a `ParsedStruct`
    ///
    /// * `decl` should be obtained using `clang::sonar::find_structs`
    fn new(decl: clang::sonar::Declaration) -> ParsedStruct {
        let record_type = decl.entity.get_type().expect("struct type missing somehow");
        ParsedStruct {
            name: decl.name,
            fields: decl.entity.get_children().into_iter().map(|f| ParsedField::new(&record_type, f)).collect(),
        }
    }
    /// Serializes fields for the body of a case class declaration
    fn case_class_fields(&self) -> String {
        let fs: Vec<String> = self.fields.iter().map(|f| format!("{}:{}", f.name, f.java_type)).collect();
        fs.join(",")
    }
    /// Serializes fields to initialize a case class from a Javolution object
    fn case_class_constructor(&self) -> String {
        let fs: Vec<String> = self.fields.iter().map(|f| format!("record.{}.get()", f.name)).collect();
        fs.join(",")
    }
}

/// * `tu` should be the top-level parsed construct
/// produced by Clang
fn analyze(tu: clang::TranslationUnit) -> Analysis {
    Analysis {
        structs: clang::sonar::find_structs(tu.get_entity().get_children())
            .into_iter()
            .map(ParsedStruct::new)
            .collect()
    }
}

/// The real main, this attempts to provide informative error
/// messages if stuff goes wrong during parsing, etc.
fn run() -> Result<(), String> {
    let args = clap_app!(structparse =>
                         (version: "0.2")
                         (author: "Alexander Lemon")
                         (about: "Generates Spark library code from C Structs")
                         (@arg INPUT: +required "Header file with struct definitions to use")
                         (@arg OUTPUT: +required "Where to put the output")
                        ).get_matches();
    let c_lock = clang::Clang::new()?;
    let index = clang::Index::new(&c_lock, true, true);
    let mut outbuf = std::fs::OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .open(args.value_of("OUTPUT").unwrap())
        .map_err(|e| format!("File open error: {}", e))?;

    let data = analyze(index.parser(args.value_of("INPUT").unwrap()).parse()?);
    let r = data.render().map_err(|e| format!("render error: {}", e))?;
    outbuf.write_all(&r.into_bytes()).map_err(|e| format!("output error: {}", e))?;

    Ok(())
}

/// Wrapper for error handling
fn main() {
    match run() {
        Ok(_) => println!("______________________\n        Parse Complete"),
        Err(err) => println!("Runtime error: {}", err)
    }
}
