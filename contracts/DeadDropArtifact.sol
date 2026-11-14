// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/token/ERC721/extensions/ERC721URIStorage.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/// @notice Minimal Amoy-ready collectible contract. Only the trusted discovery operator mints.
/// @dev Deploy and audit before use; keep the owner key in a backend signer, never in the Android app.
contract DeadDropArtifact is ERC721URIStorage, Ownable {
    uint256 private _nextTokenId = 1;
    mapping(bytes32 => bool) public claimedDiscoveries;

    error DiscoveryAlreadyClaimed(bytes32 discoveryId);
    error InvalidRecipient();
    error InvalidMetadataUri();

    constructor(address operator) ERC721("DeadDrop Artifact", "DROP") Ownable(operator) {}

    function mintValidatedDiscovery(
        address recipient,
        string calldata metadataUri,
        bytes32 discoveryId
    ) external onlyOwner returns (uint256 tokenId) {
        if (recipient == address(0)) revert InvalidRecipient();
        if (bytes(metadataUri).length == 0) revert InvalidMetadataUri();
        if (claimedDiscoveries[discoveryId]) revert DiscoveryAlreadyClaimed(discoveryId);

        claimedDiscoveries[discoveryId] = true;
        tokenId = _nextTokenId++;
        _safeMint(recipient, tokenId);
        _setTokenURI(tokenId, metadataUri);
    }
}
