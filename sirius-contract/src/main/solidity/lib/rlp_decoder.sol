pragma solidity ^0.5.1;

import "./byte_util.sol";

//rlp to type
library RLPDecoder {

    uint constant DATA_SHORT_START = 0x80;
    uint constant DATA_LONG_START = 0xB8;
    uint constant LIST_SHORT_START = 0xC0;
    uint constant LIST_LONG_START = 0xF8;

    uint constant DATA_LONG_OFFSET = 0xB7;
    uint constant LIST_LONG_OFFSET = 0xF7;

    /* Iterator */
    function next(RLPLib.Iterator memory self) internal pure returns (RLPLib.RLPItem memory subItem) {
        if(hasNext(self)) {
            uint ptr = self._unsafe_nextPtr;
            uint itemLength = _itemLength(ptr);
            subItem._unsafe_memPtr = ptr;
            subItem._unsafe_length = itemLength;
            self._unsafe_nextPtr = ptr + itemLength;
        }
        else
            revert();
    }

    function next(RLPLib.Iterator memory self, bool strict) internal pure returns (RLPLib.RLPItem memory subItem) {
        subItem = next(self);
        if(strict && !_validate(subItem))
            revert();
        return subItem;
    }

    function hasNext(RLPLib.Iterator memory self) internal pure returns (bool) {
        return self._unsafe_nextPtr < self._unsafe_item._unsafe_memPtr + self._unsafe_item._unsafe_length;
    }

    /* RLPItem */

    /// @dev Creates an RLPItem from an array of RLP encoded bytes.
    /// @param self The RLP encoded bytes.
    /// @return An RLPItem
    function toRLPItem(bytes memory self) internal pure returns (RLPLib.RLPItem memory) {
        uint len = self.length;
        if (len == 0) {
            return RLPLib.RLPItem(0, 0);
        }
        uint memPtr;
        assembly {
            memPtr := add(self, 0x20)
        }
        return RLPLib.RLPItem(memPtr, len);
    }

    /// @dev Creates an RLPItem from an array of RLP encoded bytes.
    /// @param self The RLP encoded bytes.
    /// @param strict Will revert() if the data is not RLP encoded.
    /// @return An RLPItem
    function toRLPItem(bytes memory self, bool strict) internal pure returns (RLPLib.RLPItem memory) {
        RLPLib.RLPItem memory item = toRLPItem(self);
        if(strict) {
            uint len = self.length;
            if(_payloadOffset(item) > len)
                revert();
            if(_itemLength(item._unsafe_memPtr) != len)
                revert();
            if(!_validate(item))
                revert();
        }
        return item;
    }

    /// @dev Check if the RLP item is null.
    /// @param self The RLP item.
    /// @return 'true' if the item is null.
    function isNull(RLPLib.RLPItem memory self) internal pure returns (bool ret) {
        return self._unsafe_length == 0;
    }

    /// @dev Check if the RLP item is a list.
    /// @param self The RLP item.
    /// @return 'true' if the item is a list.
    function isList(RLPLib.RLPItem memory self) internal pure returns (bool ret) {
        if (self._unsafe_length == 0)
            return false;
        uint memPtr = self._unsafe_memPtr;
        assembly {
            ret := iszero(lt(byte(0, mload(memPtr)), 0xC0))
        }
    }

    /// @dev Check if the RLP item is empty (string or list).
    /// @param self The RLP item.
    /// @return 'true' if the item is null.
    function isEmpty(RLPLib.RLPItem memory self) internal pure returns (bool ret) {
        if(isNull(self))
            return false;
        uint b0;
        uint memPtr = self._unsafe_memPtr;
        assembly {
            b0 := byte(0, mload(memPtr))
        }
        return (b0 == DATA_SHORT_START || b0 == LIST_SHORT_START);
    }

    /// @dev Get the number of items in an RLP encoded list.
    /// @param self The RLP item.
    /// @return The number of items.
    function items(RLPLib.RLPItem memory self) internal pure returns (uint) {
        if (!isList(self))
            return 0;
        uint b0;
        uint memPtr = self._unsafe_memPtr;
        assembly {
            b0 := byte(0, mload(memPtr))
        }
        uint pos = memPtr + _payloadOffset(self);
        uint last = memPtr + self._unsafe_length - 1;
        uint itms;
        while(pos <= last) {
            pos += _itemLength(pos);
            itms++;
        }
        return itms;
    }

    /// @dev Create an iterator.
    /// @param self The RLP item.
    /// @return An 'Iterator' over the item.
    function iterator(RLPLib.RLPItem memory self) internal pure returns (RLPLib.Iterator memory it) {
        if (!isList(self))
            revert();
        uint ptr = self._unsafe_memPtr + _payloadOffset(self);
        it._unsafe_item = self;
        it._unsafe_nextPtr = ptr;
    }

    /// @dev Get the list of sub-items from an RLP encoded list.
    /// Warning: This is inefficient, as it requires that the list is read twice.
    /// @param self The RLP item.
    /// @return Array of RLPItems.
    function toList(RLPLib.RLPItem memory self) internal pure returns (RLPLib.RLPItem[] memory list) {
        if(!isList(self))
            revert();
        uint numItems = items(self);
        list = new RLPLib.RLPItem[](numItems);
        RLPLib.Iterator memory it = iterator(self);
        uint idx;
        while(hasNext(it)) {
            list[idx] = next(it);
            idx++;
        }
    }

    /// @dev Decode an RLPItem into a uint. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toUint(RLPLib.RLPItem memory self) internal pure returns (uint data) {
        if(!RLPLib.isData(self))
            revert();
        uint rStartPos;
        uint len;
        (rStartPos, len) = RLPLib._decode(self);
        if (len > 32)
            revert();
        assembly {
            data := div(mload(rStartPos), exp(256, sub(32, len)))
        }
    }

    /// @dev Decode an RLPItem into a boolean. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toBool(RLPLib.RLPItem memory self) internal pure returns (bool data) {
        if(!RLPLib.isData(self))
            revert();
        uint rStartPos;
        uint len;
        (rStartPos, len) = RLPLib._decode(self);
        if (len != 1)
            revert();
        uint temp;
        assembly {
            temp := byte(0, mload(rStartPos))
        }
        if (temp > 1)
            revert();
        return temp == 1 ? true : false;
    }

    /// @dev Decode an RLPItem into a byte. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toByte(RLPLib.RLPItem memory self) internal pure returns (byte data) {
        if(!RLPLib.isData(self))
            revert();
        uint rStartPos;
        uint len;
        (rStartPos, len) = RLPLib._decode(self);
        if (len != 1)
            revert();
        byte temp;
        assembly {
            temp := byte(0, mload(rStartPos))
        }
        return byte(temp);
    }

    /// @dev Decode an RLPItem into an int. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toInt(RLPLib.RLPItem memory self) internal pure returns (int data) {
        return int(toUint(self));
    }

    /// @dev Decode an RLPItem into a bytes32. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toBytes32(RLPLib.RLPItem memory self) internal pure returns (bytes32 data) {
        return bytes32(toUint(self));
    }

    /// @dev Decode an RLPItem into an address. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toAddress(RLPLib.RLPItem memory self) internal pure returns (address data) {
        if(!RLPLib.isData(self))
            revert();
        uint rStartPos;
        uint len;
        (rStartPos, len) = RLPLib._decode(self);
        if (len != 20)
            revert();
        assembly {
            data := div(mload(rStartPos), exp(256, 12))
        }
    }

    /// @dev Decode an RLPItem into an ascii string. This will not work if the
    /// RLPItem is a list.
    /// @param self The RLPItem.
    /// @return The decoded string.
    function toAscii(RLPLib.RLPItem memory self) internal pure returns (string memory str) {
        if(!RLPLib.isData(self))
            revert();
        uint rStartPos;
        uint len;
        (rStartPos, len) = RLPLib._decode(self);

        bytes memory bts = ByteUtilLib._copyToBytes(rStartPos, len);
        str = string(bts);
    }

    // Get the payload offset.
    function _payloadOffset(RLPLib.RLPItem memory self) private pure returns (uint) {
        if(self._unsafe_length == 0)
            return 0;
        uint b0;
        uint memPtr = self._unsafe_memPtr;
        assembly {
            b0 := byte(0, mload(memPtr))
        }
        if(b0 < DATA_SHORT_START)
            return 0;
        if(b0 < DATA_LONG_START || (b0 >= LIST_SHORT_START && b0 < LIST_LONG_START))
            return 1;
        if(b0 < LIST_SHORT_START)
            return b0 - DATA_LONG_OFFSET + 1;
        return b0 - LIST_LONG_OFFSET + 1;
    }

    // Get the full length of an RLP item.
    function _itemLength(uint memPtr) private pure returns (uint len) {
        uint b0;
        assembly {
            b0 := byte(0, mload(memPtr))
        }
        if (b0 < DATA_SHORT_START)
            len = 1;
        else if (b0 < DATA_LONG_START)
            len = b0 - DATA_SHORT_START + 1;
        else if (b0 < LIST_SHORT_START) {
            assembly {
                let bLen := sub(b0, 0xB7) // bytes length (DATA_LONG_OFFSET)
                let dLen := div(mload(add(memPtr, 1)), exp(256, sub(32, bLen))) // data length
                len := add(1, add(bLen, dLen)) // total length
            }
        }
        else if (b0 < LIST_LONG_START)
            len = b0 - LIST_SHORT_START + 1;
        else {
            assembly {
                let bLen := sub(b0, 0xF7) // bytes length (LIST_LONG_OFFSET)
                let dLen := div(mload(add(memPtr, 1)), exp(256, sub(32, bLen))) // data length
                len := add(1, add(bLen, dLen)) // total length
            }
        }
    }

    // Check that an RLP item is valid.
    function _validate(RLPLib.RLPItem memory self) private pure returns (bool ret) {
        // Check that RLP is well-formed.
        uint b0;
        uint b1;
        uint memPtr = self._unsafe_memPtr;
        assembly {
            b0 := byte(0, mload(memPtr))
            b1 := byte(1, mload(memPtr))
        }
        if(b0 == DATA_SHORT_START + 1 && b1 < DATA_SHORT_START)
            return false;
        return true;
    }
}

