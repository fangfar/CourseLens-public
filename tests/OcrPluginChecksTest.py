import unittest
from OcrPluginChecks import inspect


class SeparationTests(unittest.TestCase):
    def test_isolation_and_signer(self):
        host = ('cn.courselens', 'trusted', ['classes.dex'])
        plugin = ('cn.courselens.ocr', 'trusted', ['lib/arm64-v8a/libmlkit_google_ocr_pipeline.so', 'assets/mlkit-google-ocr-models/model'])
        inspect(host, plugin, 'trusted')
        inspect((host[0], host[1], plugin[2]), plugin, 'trusted', bundled=True)
        with self.assertRaises(AssertionError):
            inspect(host, plugin, 'trusted', bundled=True)
        for h, p, certificate in [(host, (plugin[0], 'other', plugin[2]), 'trusted'),
                                  ((host[0], host[1], ['lib/arm64-v8a/libmlkit_google_ocr_pipeline.so']), plugin, 'trusted'),
                                  (host, ('cn.courselens.ocr.qa', *plugin[1:]), 'trusted'),
                                  (host, plugin, 'other'), (host, (*plugin[:2], []), 'trusted')]:
            with self.assertRaises(AssertionError):
                inspect(h, p, certificate)


if __name__ == '__main__':
    unittest.main()
