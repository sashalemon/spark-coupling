void courier_write_table(const char* name, void* rows, size_t row_size, size_t nrows, int append);
char* courier_request_analysis(const char* master, const char* analysis, const char** data_deps, size_t ndeps);
void courier_free_data(char* data);
