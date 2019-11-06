//! The external interface of libcourier
//! 
//! Contains data-transfer code as well as a simple client for
//! the test RPC protocol
//!
//! Currently, this library is stateless. Stateful behavior might be
//! more efficient, but that's for future work
use libc::{c_char, c_int, c_void, size_t};
use std::ffi::{CStr, CString};
use std::io::{Read, Write};
use std::net::{TcpStream, Shutdown};
use std::{thread, time};
use hyper::client::Client;
use hyper::header::ContentType;
use hyper::status::StatusCode;
use json::{stringify, JsonValue};
use ring::Ring;

fn s_of_c(ptr: *const c_char) -> String {
    unsafe { CStr::from_ptr(ptr).to_string_lossy().into_owned() }
}

fn wait_for_host(host: &str) {
    loop {
        match TcpStream::connect(host) {
            Ok(con) => {con.shutdown(Shutdown::Both).unwrap(); break},
            Err(_) => thread::sleep(time::Duration::from_millis(100))
        }
    }
}

/// * Writes a row-oriented dataset named `name_ptr` into Spark
/// * `rows` points to the dataset, 
/// * `row_size` is the length in bytes of one row
/// * `nrows` is the number of rows
/// * `append` 0 to create a new DataFrame; 1 to append to existing data if any 
#[no_mangle]
pub fn courier_write_table(name_ptr: *const c_char, 
                           rows: *mut c_void, 
                           row_size: size_t, 
                           nrows: size_t,
                           append: c_int) 
{
    let rows_per_block = 16384 * 16; //if nrows > (1024 as usize).pow(2) { nrows / 128 } else if nrows > 64 { nrows / 64 } else { 512 };
    let nblocks = 4;
    let mut ring = Ring::new(row_size, rows_per_block, nblocks);
    let name = s_of_c(name_ptr);

    wait_for_host("127.0.0.1:8081");
    let res = Client::new()
        .post("http://127.0.0.1:8081/data")
        .header(ContentType(mime!(Application/Json)))
        .body(stringify(object!{
            "schemaName" => name,
            "rowsPerBlock" => rows_per_block as i64,
            "nblocks" => nblocks as i64,
            "nrows" => nrows as i64,
            "append" => append != 0
        }).as_str())
        .send()
        .unwrap();
    assert_eq!(res.status, StatusCode::Accepted);

    let buf = unsafe {
        ::std::slice::from_raw_parts_mut(rows as *mut u8, row_size*nrows)
    };
    ring.write_all(&buf).unwrap();
    ring.finalize();
}

/// * Does RPC with the RPC server at `master_addr_ptr`
/// * Requests an analysis according to the string `analysis_ptr`
/// * `data_deps` is a list of names of data the analysis depends on. May be null if `ndeps` is 0.
/// * `ndeps` is the size of `data_deps`, and may be 0
///
/// # Safety
/// The caller must give the return value to `courier_free_data`
/// in order to avoid memory leaks
#[no_mangle]
pub fn courier_request_analysis(master_addr_ptr: *const c_char, 
                                analysis_ptr: *const c_char, 
                                data_deps: *const *const c_char, 
                                ndeps: size_t) -> *mut c_char
{
    let master_address = s_of_c(master_addr_ptr);
    let analysis = s_of_c(analysis_ptr);
    let mut deps = JsonValue::new_array();
    for i in 0..ndeps {
        //println!("{}", s_of_c(unsafe { *data_deps.offset(i as isize) }));
        deps.push(s_of_c(unsafe { *data_deps.offset(i as isize) })).unwrap();
    }
    wait_for_host(format!("{}:8082", master_address).as_str());
    let mut res = Client::new()
        .post(format!("http://{}:8082/ready", master_address).as_str())
        .header(ContentType(mime!(Application/Json)))
        .body(stringify(object!{
            "analysis" => analysis,
            "dependencies" => deps
        }).as_str())
        .send()
        .unwrap();
    assert_eq!(res.status, StatusCode::Ok);
    let mut raw = vec![];
    res.read_to_end(&mut raw).unwrap();
    unsafe { CString::from_vec_unchecked(raw) }.into_raw()
}

/// Frees data allocated by `courier_request_analysis`
#[no_mangle]
pub fn courier_free_data(ptr: *mut c_char) {
    if ptr.is_null() { return }
    unsafe { CString::from_raw(ptr); }
}
