//! This library will enable efficient communication with Spark executors running matching server code.
//! Intended to produce a shared library libcourier.so with a C-compatible interface.
// use std::io::Write;
extern crate libc;
extern crate memmap;
extern crate hyper;
#[macro_use] extern crate mime;
#[macro_use(object)] extern crate json;

pub mod ring;
pub mod courier;
