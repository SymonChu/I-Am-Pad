#![allow(unsafe_op_in_unsafe_fn)]

mod art;
mod lifecycle;
mod logging;
mod natives;
mod payload;
mod so_hider;
mod zygisk;

use lifecycle::ImpPadModule;
use std::ffi::c_void;

use jni::sys::JNIEnv as RawJNIEnv;
use zygisk::{AppSpecializeArgs, ModuleAbi, ServerSpecializeArgs};

use crate::zygisk::ApiTable;

extern "C" fn pre_app(m: *mut c_void, args: *mut AppSpecializeArgs) {
    unsafe { lifecycle::do_pre_app_specialize(&mut *(m as *mut ImpPadModule), args) }
}

extern "C" fn post_app(m: *mut c_void, args: *const AppSpecializeArgs) {
    unsafe { lifecycle::do_post_app_specialize(&mut *(m as *mut ImpPadModule), args) }
}

extern "C" fn pre_server(m: *mut c_void, args: *mut ServerSpecializeArgs) {
    unsafe { lifecycle::do_pre_server_specialize(&mut *(m as *mut ImpPadModule), args) }
}

extern "C" fn post_server(_m: *mut c_void, _args: *const ServerSpecializeArgs) {}

/// # Safety
///
/// Called exclusively by the Zygisk framework with a valid `api_table` and `JNIEnv`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn zygisk_module_entry(table: *mut ApiTable, env: *mut RawJNIEnv) {
    let module = Box::leak(Box::new(ImpPadModule::new(table, env)));
    let abi = Box::leak(Box::new(ModuleAbi {
        api_version: 4,
        impl_ptr: module as *mut ImpPadModule as *mut c_void,
        pre_app_specialize: pre_app,
        post_app_specialize: post_app,
        pre_server_specialize: pre_server,
        post_server_specialize: post_server,
    }));
    unsafe { ((*table).register_module)(table, abi) };
}
