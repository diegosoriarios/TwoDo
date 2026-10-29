package com.diego.twodo.domain.model

object ModelSpec {
    const val DISPLAY_NAME = "Qwen3 0.6B INT4"
    const val LICENSE = "Apache-2.0"

    const val FILE_NAME = "twodo_llm_qwen3_0_6b_int4.litertlm"
    const val DOWNLOAD_URL =
        "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/e6bb3e4a1eb3f663153f0c1c12b735d64be657a7/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
    const val SHA256 = "03e7da1eb1108b50dffaa9bb52cc7bcbad2eb0c66ca990267f480c1e545d2856"
    const val SIZE_BYTES = 344_671_744L

    val LEGACY_FILE_NAMES = listOf(
        "twodo_llm_model.bin",
        "twodo_llm_model.bin.tmp",
    )
}
