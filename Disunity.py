import sys
import struct
from pprint import pprint
from munch import Munch

class BinaryReader:

    be = False

    def __init__(self, file):
        self.file = file
        
    def tell():
        return file.tell()
        
    def seek(offset, whence=0):
        file.seek(offset, whence)
        
    def read(self, size):
        return self.file.read(size)
        
    def read_cstring(self):
        buf = bytearray()
        b = self.read_int8()
        while b and b != 0:
            buf.append(b)
            b = self.read_int8()

        return buf.decode("ascii")
        
    def read_struct(self, format):
        size = struct.calcsize(format)
        data = self.file.read(size)
        return struct.unpack(format, data)
        
    def read_int8(self):
        b = self.file.read(1)
        return b[0] if b else None
        
    def read_int32(self):
        return self.read_struct(">i" if self.be else "i")[0]
        
class SerializedFileReader:

    def read(self, file):
        r = BinaryReader(file)
        sf = SerializedFile()
        self.read_header(r, sf)
        self.read_rtti(r, sf)
        return sf

    def read_header(self, r, sf):
        # the header always uses big-endian byte order
        r.be = True
    
        sf.header.metadataSize = r.read_int32()
        sf.header.fileSize = r.read_int32()
        sf.header.version = r.read_int32()
        sf.header.dataOffset = r.read_int32()
        
        if sf.header.dataOffset > sf.header.fileSize:
            raise RuntimeError("Invalid dataOffset %d" % sf.header.dataOffset)
            
        if sf.header.metadataSize > sf.header.fileSize:
            raise RuntimeError("Invalid metadataSize %d" % sf.header.metadataSize)
            
        if sf.header.version >= 9:
            sf.header.endianness = r.read_int8()
            r.read(3) # reserved
            
        # newer formats use little endian for the rest of the file
        if sf.header.version > 5:
            r.be = False
        
        # TODO: test more formats
        if sf.header.version != 15:
            raise RuntimeError("Unsupported format version %d" % sf.header.version)
        
    def read_rtti(self, r, sf):
        # older formats store the object data before the structure data
        if sf.header.version < 9:
            rtti_offset = sf.header.fileSize - sf.header.metadataSize + 1
            r.seek(rtti_offset)
        
        if sf.header.version > 6:
            sf.rtti.signature = r.read_cstring()
            sf.rtti.attributes = r.read_int32()
            
        if sf.header.version > 13:
            sf.rtti.embedded = r.read_int8()
        
        sf.rtti.numBaseClasses = r.read_int32()
        
class SerializedFile:
    header = Munch()
    rtti = Munch()
    
def main(argv):
    app = argv.pop(0)
    path = argv.pop(0)
    
    reader = SerializedFileReader()
    
    print(path)
    with open(path, "rb") as file:
        sf = reader.read(file)
        pprint(sf.header)
        pprint(sf.rtti)

    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv))